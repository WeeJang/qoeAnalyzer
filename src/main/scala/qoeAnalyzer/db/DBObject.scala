package qoeAnalyzer.db

import com.mongodb.casbah.Imports._
import org.apache.log4j._
import qoeAnalyzer.util.Utils

import scala.xml._

object DBObject{
    lazy val logger: Logger = Logger.getLogger("logger4DBObject")
    //系统设置元素
    lazy val iniXMLElem = XML.load(this.getClass.getClassLoader.getResourceAsStream("ini.xml"))
     //数据库客户端对象
    lazy val mongoClient = MongoClient(MongoClientURI(Utils.getMongoDBUri(iniXMLElem)))
    //对应数据库名
    lazy val db = mongoClient(Utils.getMongoDBName(iniXMLElem))
    
    //dash collection
    lazy val dashCollection = db(Utils.getDashCollectionName(iniXMLElem))
    //signal collection
    lazy val signalCollection = db(Utils.getSignalCollectionName(iniXMLElem))
    
    
    //初始化
    def init() : Unit = {
      db
      logger
      logger.info("DBObject init ...")
    }
    
}