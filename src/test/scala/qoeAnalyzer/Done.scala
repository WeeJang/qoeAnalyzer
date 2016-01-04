package qoeAnalyzer

class Done {
     def done(): Unit = {
    
        
//    val oneDoc = dashCollection.findOneByID(new ObjectId("554757ad31e3d8f25b280be9")).get
//    println()
//    
    //    val allDocs = dashCollection.find()
    //    
    //    for(doc <- allDocs){
    //      println(doc)
    //    }

//    val oneDoc = dashCollection.findOneByID(new ObjectId("554757ad31e3d8f25b280be9")).get
//    //println(oneDoc.get.getClass) //class com.mongodb.BasicDBObject
//    val infoList = oneDoc.get("qosInfo").asInstanceOf[BasicDBList]
//
//    //
//    val infoListNew = infoList.map { x =>
//      {
//        val elem = x.asInstanceOf[BasicDBList](1)
//        val elem_2: Double = if (elem.isInstanceOf[Int]) elem.asInstanceOf[Int] * 1.0 else elem.asInstanceOf[Double]
//        elem_2
//      }
//    }
//
//    val diff = infoListNew.foldLeft(List[Double]()) { (a: List[Double], b: Double) =>
//      {
//        val new_a: List[Double] = {
//          if (a.size == 0) List(b)
//          else {
//            val delt: Double = b - a.head
//            val ret: List[Double] = b :: (a.tail :+ delt)
//            ret
//          }
//        }
//        new_a
//      }
//    }
//
//    diff.map { println _ }
//
//    println(diff.size)

  }
}