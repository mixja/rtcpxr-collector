package io.caerulean.sipstack

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.sipstack.netty.codec.sip.SipMessageEvent
import rx.lang.scala.{Observer, Observable}
import rx.lang.scala.subjects.PublishSubject

/**
 * Created by jmenga on 23/02/15.
 */
@Sharable
final class UASHandler extends SimpleChannelInboundHandler[SipMessageEvent] {

  private val subject = PublishSubject[SipMessageEvent]()

  override def channelRead0(ctx: ChannelHandlerContext, msg: SipMessageEvent): Unit = {

    val sipMsg = msg.getMessage

    println("*** UASHandler received message ***")
    // Filter messages to only RTCP-XR VQ reports
    val contentType = Option(sipMsg.getContentTypeHeader)
      .filter(_.getValue.toString.toLowerCase() == "application/vq-rtcpxr")

    val response = contentType
      .map(_ => {
        subject.onNext(msg)
        sipMsg.createResponse(200)
      })
      .getOrElse(sipMsg.createResponse(401))
    msg.getConnection().send(response)

    println()
    println("*** UASHandler sent response *** ")
    println(response)
    for (i <- 1 to 2) println()
  }

  def subscribe(observer: Observer[SipMessageEvent]): Unit = {
    subject.subscribe(observer)
  }
}
