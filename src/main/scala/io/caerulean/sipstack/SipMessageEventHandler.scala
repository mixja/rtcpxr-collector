package io.caerulean.sipstack

import io.pkts.packet.sip.SipMessage
import io.sipstack.netty.codec.sip.SipMessageEvent
import org.joda.time.format.DateTimeFormat
import org.vertx.scala.core.json.Json
import rx.lang.scala.Observer
import scala.util.Try
import scalaj.http.Http

/**
 * Created by jmenga on 24/02/15.
 */

class SipMessageEventHandler() extends Observer[SipMessageEvent] {
  override def onNext(value: SipMessageEvent): Unit = {
    val msg = value.getMessage
    val remoteHost = value.getConnection.getRemoteIpAddress

    println("SipMessageHandler received a message from " + remoteHost + " -> " + msg)

    // Parse body and convert to RTCP-XR Report
    val result = parseMessage(msg, remoteHost)

    // PUT report to Web Service
    result map { putReport _ }

  }

  override def onError(error: Throwable): Unit = {
    println("SipMessageHandler received an error -> " + error.getMessage)
  }

  def putReport(report: VoiceQualityMessage): Unit = {

    val request = Http("http://localhost:9200/vquality/reports/" + report.callId)
        .header("Content-Type", "application/json")
        .method("PUT")

    JsonUtils.toJson(report) map { json =>
      println("Result -> " + json)
      val response = request.postData(json).asString
      println(response)
    }

//    val request = url("http://localhost:9200/vquality/reports/" + report.callId)
//    val put = request.PUT
//    JsonUtils.toJson(report) map { json =>
//      println("Result -> " + json)
//      put.addHeader("Content-Type", "application/json")
//      put.addBodyPart(new StringPart("body", json))
//    }
//
//    val response = Http(request OK as.String)
//    for (r <- response)
//      println(r)
  }

  /**
   * Parses and validates a SIP message and returns a [[VoiceQualityReport]] [[Option]]
   * @param message
   * @return
   */
  def parseMessage(message: SipMessage, remoteHost: String) : Option[VoiceQualityMessage] = {
    val fromAddress = message.getFromHeader.getAddress.getURI.toString
    val fromTuple = extractFromSipUri(fromAddress)
    val toAddress = Try(message.getToHeader.getAddress.getURI.toString).toOption
    val userAgent = Try(message.getHeader("User-Agent").getValue.toString).toOption
    val body = Try(message.getContent.toString).toOption

    val report = for {
      bodyString <- body
      report <- parseBody(bodyString)
    } yield report

    report.map(r => VoiceQualityMessage(r.callId, remoteHost, fromAddress, fromTuple._1, fromTuple._2, toAddress,userAgent,r))
  }

  /**
   * Extracts username and realm from a SIP URI
   * @param uri
   * @return Tuple containing username and realm
   */
  def extractFromSipUri(uri: String) : Tuple2[Option[String],Option[String]] = {
    val segments = uri.split("@")
    val user = Try(segments(0).split(":").last).toOption
    val realm = Try(segments(1)).toOption
    (user,realm)
  }

  /**
   * Example SDP Payload
   * VQSessionReport: CallTerm
   * LocalMetrics:
   * Timestamps:START=2015-02-23T02:35:01Z STOP=2015-02-23T02:35:46Z
   * SessionDesc:PT=9 PPS=50 PLC=1 SSUP=off
   * CallID:329662837@192.168.1.21
   * LocalAddr:IP=192.168.1.21 PORT=11784 SSRC=0x1a2a9c52
   * RemoteAddr:IP=114.23.7.184 PORT=27210 SSRC=0xce2256d2
   * JitterBuffer:JBA=2 JBR=15 JBN=40 JBM=40 JBX=240
   * PacketLoss:NLR=0.0 JDR=0.0
   * BurstGapLoss:BLD=0.7 BD=60 GLD=0.0 GD=30000 GMIN=16
   * Delay:RTD=0 ESD=177 SOWD=88 IAJ=0 MAJ=0
   * Signal:SL=-21 NL=-64 RERL=127
   * QualityEst:RCQ=92 EXTRI=127 MOSLQ=3.8 MOSCQ=3.8
   * DialogID:329662837@192.168.1.21;to-tag=55cUUK26KmU4j;from-tag=2606093232
   *
   * @param body
   * @return
   */
  def parseBody(body: String) : Option[VoiceQualityReport] = {
    val fields = extractFields(body)
    val vqSessionReport = fields.get("vqsessionreport")
    val localMetrics = fields.get("localmetrics")
    val timeStamps = fields.get("timestamps").flatMap(toTimestamps(_))
    val callId = fields.get("callid")
    val dialogId = fields.get("dialogid")
    val sessionDesc = fields.get("sessiondesc").flatMap(toObject[SessionDesc](_))
    val localAddr = fields.get("localaddr").flatMap(toObject[MediaEndpoint](_))
    val remoteAddr = fields.get("remoteaddr").flatMap(toObject[MediaEndpoint](_))
    val jitterBuffer = fields.get("jitterbuffer").flatMap(toObject[JitterBuffer](_))
    val packetLoss = fields.get("packetloss").flatMap(toObject[PacketLoss](_))
    val burstGapLoss = fields.get("burstgaploss").flatMap(toObject[BurstGapLoss](_))
    val delay = fields.get("delay").flatMap(toObject[Delay](_))
    val signal = fields.get("signal").flatMap(toObject[Signal](_))
    val qualityEst = fields.get("qualityest").flatMap(toObject[QualityEst](_))

    // Create report
    // VQSessionReport and CallID fields are mandatory
    // All other fields are options
    for {
      sessionReport <- vqSessionReport
      cid <- callId
    } yield VoiceQualityReport(
      sessionReport, cid, localMetrics, timeStamps, sessionDesc, dialogId, localAddr,
      remoteAddr, jitterBuffer, packetLoss, burstGapLoss, delay, signal, qualityEst)
  }

  /**
   * Converts multi-line body of text to a [[Map]] object
   * Each line of text must be formatted as key: value
   * The key is converted to lowercase and the value trimmed of whitespace
   * e.g.
   *
   * VQSessionReport: stringValue1
   * LocalMetrics: stringValue2
   *
   * will be converted to Map("vqsessionreport" -> stringValue1, "localmetrics" -> stringValue2)
   * @param body
   * @return
   */
  def extractFields(body: String) : Map[String,String] = {
    val lines = body.split("\n")
    lines.map(line => {
      val index = line.indexOf(':')
      line.substring(0, index).toLowerCase -> line.substring(index + 1).trim
    }).toMap
  }

  /**
   * Converts k=v property strings to a [[Timestamps]] object
   * For Timestamps class we need to convert date string values to epoch time
   * @param property
   * @return
   */
  def toTimestamps(property: String) : Option[Timestamps] = {
    val map = toMap(property)
    val dt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    for {
      start <- map.get("start")
      stop <- map.get("stop")
    } yield Timestamps(dt.parseMillis(start), dt.parseMillis(stop))
  }

  /**
   * Converts space delimited k=v property strings to specified type
   * e.g. name=bob age=15 will be converted to Person("bob",15)
   * @param property
   * @param m
   * @tparam T
   * @return
   */
  def toObject[T](property: String)(implicit m: Manifest[T]) : Option[T] = {
    val map = toMap(property)
    val json = Json.obj(map.toSeq: _*).toString
    JsonUtils.fromJson[T](json).toOption
  }

  /**
   * Converts a string of key value pairs to a Map
   * e.g. k1=v1 k2=v2 is converted to Map(k1 -> v1, k2 -> v2)
   * N.b. all key values will be converted to a lowercase
   * @param property
   * @return
   */
  def toMap(property: String) : Map[String,String] = {
    val fields = property.split(" ")
    // Array of [X=value,Y=value,...]
    fields.map(field => {
      val segments = field.split('=')
      segments.head.toLowerCase -> segments.last
    }).toMap
  }
}


