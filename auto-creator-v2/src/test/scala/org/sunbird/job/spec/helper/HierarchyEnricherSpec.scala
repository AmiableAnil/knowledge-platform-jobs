package org.sunbird.job.spec.helper

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.sunbird.job.helpers.HierarchyEnricher
import org.sunbird.job.model.ObjectData
import org.sunbird.job.task.AutoCreatorV2Config
import org.sunbird.job.util.JSONUtil

class HierarchyEnricherSpec extends FlatSpec with BeforeAndAfterAll with Matchers with MockitoSugar {

  val config: Config = ConfigFactory.load("test.conf").withFallback(ConfigFactory.systemEnvironment())
  val jobConfig: AutoCreatorV2Config = new AutoCreatorV2Config(config)

  "getChildren" should "return the children map for the given object" in {
    val data = new ObjectData("do_113264102878707712117", "Content", Map(), Some(Map()), Some(getHierarchy()))
    val result = new TestHierarchyEnricher().getChildren(data)(jobConfig)
    val question = result.getOrElse("do_113264104174723072120", Map()).asInstanceOf[Map[String, AnyRef]]
    question.getOrElse("objectType", "").asInstanceOf[String] shouldEqual "Question"
    question.getOrElse("downloadUrl", "").asInstanceOf[String] shouldEqual "https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264104174723072120/test-1_1619580036855_do_113264104174723072120_9.ecar"
  }

  "enrichHierarchy" should "enrich hierarchy of the given object" in {
    val data = new ObjectData("do_113264102878707712117", "QuestionSet", Map(), Some(Map()), Some(getHierarchy()))
    val children: Map[String, ObjectData] = Map()
    val result = new TestHierarchyEnricher().enrichHierarchy(data, children)(jobConfig)
    val extData = result.extData.getOrElse(Map())
    val hierarchy = extData.getOrElse("hierarchy", Map()).asInstanceOf[Map[String, AnyRef]]
    hierarchy.getOrElse("identifier", "").asInstanceOf[String] shouldEqual "do_113264102878707712117"
    hierarchy.getOrElse("objectType", "").asInstanceOf[String] shouldEqual "QuestionSet"
  }

  def getHierarchy(): Map[String, AnyRef] = {
    JSONUtil.deserialize[Map[String, AnyRef]]("""{"copyright":"NIT123","previewUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264102878707712117/do_113264102878707712117_html_1619580041745.html","keywords":["Test"],"subject":["English"],"channel":"01309282781705830427","downloadUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264102878707712117/test-questionset_1619580041510_do_113264102878707712117_4_SPINE.ecar","language":["English"],"mimeType":"application/vnd.sunbird.questionset","showHints":"No","variants":{"spine":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264102878707712117/test-questionset_1619580041510_do_113264102878707712117_4_SPINE.ecar","online":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264102878707712117/test-questionset_1619580041645_do_113264102878707712117_4_ONLINE.ecar"},"objectType":"QuestionSet","gradeLevel":["Class 10"],"primaryCategory":"Practice Question Set","children":[{"copyright":"NIT123","lastStatusChangedOn":"2021-04-28T08:50:40.295+0530","parent":"do_113264102878707712117","author":"Test","name":"Test 1","createdOn":"2021-04-23T04:49:01.641+0000","channel":"01309282781705830427","lastUpdatedOn":"2021-04-28T08:50:40.295+0530","subject":["English"],"showTimer":"No","identifier":"do_113264104174723072120","gradeLevel":["Class 10"],"compatibilityLevel":4,"audience":["Student"],"primaryCategory":"Multiple Choice Question","downloadUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264104174723072120/test-1_1619580036855_do_113264104174723072120_9.ecar","medium":["English"],"interactionTypes":["choice"],"framework":"ekstep_ncert_k-12","versionKey":"1619153407815","mimeType":"application/vnd.sunbird.question","code":"781740e0-4257-2769-4bba-e4c53a37df52","license":"CC BY 4.0","version":1,"templateId":"mcq-vertical","language":["English"],"board":"CBSE","showFeedback":"No","objectType":"Question","status":"Live","contentEncoding":"gzip","depth":1,"se_FWIds":["ekstep_ncert_k-12"],"allowAnonymousAccess":"Yes","contentDisposition":"inline","visibility":"Parent","qType":"MCQ","showSolutions":"No","variants":"{\"full\":\"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264104174723072120/test-1_1619580036855_do_113264104174723072120_9.ecar\",\"online\":\"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264104174723072120/test-1_1619580040129_do_113264104174723072120_9_ONLINE.ecar\"}","index":1,"pkgVersion":9}],"contentEncoding":"gzip","showSolutions":"No","identifier":"do_113264102878707712117","audience":["Student"],"visibility":"Default","showTimer":"Yes","author":"Test","consumerId":"273f3b18-5dda-4a27-984a-060c7cd398d3","childNodes":["do_113264104174723072120"],"languageCode":["en"],"version":1,"license":"CC BY 4.0","lastPublishedOn":"2021-04-28T08:50:41.325+0530","name":"Test QuestionSet","status":"Live","code":"3d36b9ce-a56c-6ee8-d35f-f2a5eeafaea7","allowSkip":"Yes","containsUserData":"No","description":"Test","medium":["English"],"createdOn":"2021-04-23T04:46:23.437+0000","pdfUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/questionset/do_113264102878707712117/do_113264102878707712117_pdf_1619580041745.pdf","contentDisposition":"inline","lastUpdatedOn":"2021-04-28T08:50:43.165+0530","allowAnonymousAccess":"Yes","lastStatusChangedOn":"2021-04-28T08:50:43.165+0530","createdFor":["01309282781705830427"],"requiresSubmit":"No","se_FWIds":["ekstep_ncert_k-12"],"setType":"materialised","pkgVersion":4,"versionKey":"1619153418829","showFeedback":"No","framework":"ekstep_ncert_k-12","depth":0,"createdBy":"5a587cc1-e018-4859-a0a8-e842650b9d64","compatibilityLevel":5,"navigationMode":"non-linear","timeLimits":{"maxTime":"3600"},"shuffle":true,"board":"CBSE"}""")
  }
}

class TestHierarchyEnricher extends HierarchyEnricher
