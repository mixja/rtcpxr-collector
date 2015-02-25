package io.caerulean.sipstack

case class VoiceQualityMessage(
  callId: String,
  remoteHost: String,
  fromAddress: String,
  user: Option[String],
  realm: Option[String],
  toAddress: Option[String],
  userAgent: Option[String],
  report: VoiceQualityReport
)

case class VoiceQualityReport(
  vqSessionReport: String,
  callId: String,
  localMetrics: Option[String],
  timestamps: Option[Timestamps],
  sessionDesc: Option[SessionDesc],
  dialogId: Option[String],
  localAddr: Option[MediaEndpoint],
  remoteAddr: Option[MediaEndpoint],
  jitterBuffer: Option[JitterBuffer],
  packetLoss: Option[PacketLoss],
  burstGapLoss: Option[BurstGapLoss],
  delay: Option[Delay],
  signal: Option[Signal],
  qualityEst: Option[QualityEst]
)

case class Timestamps (
  start: Long,
  stop: Long
)

case class SessionDesc (
  pt: Int,
  pps: Int,
  plc: Int,
  ssup: String
)

case class MediaEndpoint(
  ip: String,
  port: Int,
  ssrc: String
)

case class JitterBuffer(
  jba: Int,
  jbr: Int,
  jbn: Int,
  jbm: Int,
  jbx: Int
)

case class PacketLoss(
  nlr: Double,
  jdr: Double
)

case class BurstGapLoss(
  bld: Double,
  bd: Int,
  gld: Double,
  gd: Int,
  gmin: Int
)

case class Delay(
  rtd: Int,
  esd: Int,
  sowd: Int,
  iaj: Int,
  maj: Int
)

case class Signal(
  sl: Int,
  nl: Int,
  rerl: Int
)

case class QualityEst(
  rcg: Int,
  extri: Int,
  moslq: Double,
  moscq: Double
)
