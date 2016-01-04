package qoeAnalyzer.message

sealed trait DataMessage

//calculate
case object Calculate extends DataMessage

//wake
case object Wake extends DataMessage

//stop
case object Stop extends DataMessage

//startIndex
case class LastIndexMessage(index: Double) extends DataMessage
////wifi
//case class WiFiMessage(timeStamp:Double,RSSI:Int,linkSpeed:Int) extends DataMessage
////ping
//case class PingMessage(timeStamp:Double,rtt: Double)extends DataMessage

//wifi signal
case class wifiSignalMessage(wifiInfo: (Double, Double, Double), pingInfo: (Double, Double)) extends DataMessage

//lte signal
case class lteSignalMessage(lteInfo:(Double,Double,Double,Double,Double,Double),pingInfo:(Double,Double)) extends DataMessage

//dash with wifi signal
case class dashJoinWifiMessage(wifiInfo: (Double, Double, Double), pingInfo: (Double, Double),
  dashTuple6: (Double, Double, Double, Double, Double, Double)) extends DataMessage

//dash with lte signal
case class dashJoinLteMessage(lteInfo:(Double,Double,Double,Double,Double,Double),pingInfo:(Double,Double),
    dashTuple6: (Double, Double, Double, Double, Double, Double))extends DataMessage

//debug
case class Debug(index: Double) extends DataMessage

//test
case class Test() extends DataMessage