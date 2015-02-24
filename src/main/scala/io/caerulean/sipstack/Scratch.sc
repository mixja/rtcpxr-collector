import io.caerulean.sipstack._
import org.joda.time.format.DateTimeFormat
import org.vertx.scala.core.json.Json

//val body = "VQSessionReport: CallTerm\r\nLocalMetrics:\r\nTimestamps:START=2015-02-23T02:35:01Z STOP=2015-02-23T02:35:46Z\r\n"
val body = "VQSessionReport: CallTerm\r\nLocalMetrics:\r\nTimestamps:START=2015-02-23T02:35:01Z STOP=2015-02-23T02:35:46Z\r\nSessionDesc:PT=9 PPS=50 PLC=1 SSUP=off\r\nCallID:329662837@192.168.1.21\r\nLocalAddr:IP=192.168.1.21 PORT=11784 SSRC=0x1a2a9c52\r\nRemoteAddr:IP=114.23.7.184 PORT=27210 SSRC=0xce2256d2\r\nJitterBuffer:JBA=2 JBR=15 JBN=40 JBM=40 JBX=240\r\nPacketLoss:NLR=0.0 JDR=0.0\r\nBurstGapLoss:BLD=0.7 BD=60 GLD=0.0 GD=30000 GMIN=16\r\nDelay:RTD=0 ESD=177 SOWD=88 IAJ=0 MAJ=0\r\nSignal:SL=-21 NL=-64 RERL=127\r\nQualityEst:RCQ=92 EXTRI=127 MOSLQ=3.8 MOSCQ=3.8\r\nDialogID:329662837@192.168.1.21;to-tag=55cUUK26KmU4j;from-tag=2606093232"
val lines = body.split("\n")
val fields = lines.map(line => {
  val index = line.indexOf(':')
  line.substring(0, index).toLowerCase() -> line.substring(index + 1).trim
}).toMap


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
val qualityEst = fields.get("qualityEst").flatMap(toObject[QualityEst](_))
val x = for {
  sessionReport <- vqSessionReport
} yield VoiceQualityReport(
  sessionReport, localMetrics, timeStamps, sessionDesc, callId, dialogId, localAddr,
  remoteAddr, jitterBuffer, packetLoss, burstGapLoss, delay, signal, qualityEst)

/**
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

def toObject[T](property: String)(implicit m: Manifest[T]) : Option[T] = {
  val map = toMap(property)
  val json = Json.obj(map.toSeq: _*).toString
  JsonUtils.fromJson[T](json).toOption
}

def toMap(property: String) : Map[String,String] = {
  val fields = property.split(" ")
  // Array of [X=value,Y=value,...]
  fields.map(field => {
    val segments = field.split('=')
    segments.head.toLowerCase -> segments.tail.head
  }).toMap
}