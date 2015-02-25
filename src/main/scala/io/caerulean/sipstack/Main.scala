package io.caerulean.sipstack

import java.net.InetSocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.sipstack.netty.codec.sip.{SipMessageEncoder, SipMessageDatagramDecoder}
import com.typesafe.config.ConfigFactory

/**
 * Created by jmenga on 23/02/15.
 */
object Main extends App {
  // Load configuration
  val config = ConfigFactory.load()

  // SIP User Agent Server
  val uas = new UASHandler()
  val udpGroup = new NioEventLoopGroup()

  // Subscriber a Message Handler to incoming SIP messages
  val messageHandler = new SipMessageEventHandler(config)
  uas.subscribe(messageHandler)

  // Start
  start()

  /**
   * Starts the SIP UAS
   */
  def start(): Unit = {
    val b = new Bootstrap()
    b.group(udpGroup)
      .channel(classOf[NioDatagramChannel])
      .handler(new MyChannelInitializer(uas))

    val socketAddress = new InetSocketAddress(config.getInt("uas_port"))
    val f = b.bind(socketAddress).sync()
    f.channel().closeFuture().await()
  }
}

class MyChannelInitializer(uas: UASHandler) extends ChannelInitializer[DatagramChannel] {
  override def initChannel(ch: DatagramChannel): Unit = {
    val pipeline = ch.pipeline()
    pipeline.addLast("decoder", new SipMessageDatagramDecoder())
    pipeline.addLast("encoder", new SipMessageEncoder())
    pipeline.addLast("handler", uas)
  }
}