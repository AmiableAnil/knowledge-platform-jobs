package org.sunbird.job.publish.util

import org.slf4j.LoggerFactory
import org.sunbird.job.domain.`object`.DefinitionCache
import org.sunbird.job.publish.helpers.QuestionPublisher
import org.sunbird.job.util.{CassandraUtil, Neo4JUtil}
import org.sunbird.publish.core.{DefinitionConfig, ExtDataConfig, ObjectData}
import org.sunbird.publish.util.CloudStorageUtil

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

object QuestionPublishUtil extends QuestionPublisher {

	private val pkgTypes = List("FULL", "ONLINE")

	private[this] val logger = LoggerFactory.getLogger(classOf[QuestionPublishUtil])

	def publishQuestions(identifier: String, objList: List[ObjectData])(implicit ec: ExecutionContext, neo4JUtil: Neo4JUtil, cassandraUtil: CassandraUtil, readerConfig: ExtDataConfig, cloudStorageUtil: CloudStorageUtil, definitionCache: DefinitionCache, definitionConfig: DefinitionConfig): Unit = {
		objList.foreach(qData => {
			logger.info("QuestionPublishUtil :::: publishing child question  for : " + qData.identifier)
			val obj = getObject(qData.identifier, qData.pkgVersion, readerConfig)(neo4JUtil, cassandraUtil)
			val messages: List[String] = validate(obj, obj.identifier, validateQuestion)
			if (messages.isEmpty) {
				val enrichedObj = enrichObject(obj)(neo4JUtil, cassandraUtil, readerConfig, cloudStorageUtil)
				// Generate ECAR
				logger.info("Ecar generation for Question: " + enrichedObj.identifier)
				val objWithEcar = generateECAR(enrichedObj, pkgTypes)(ec, cloudStorageUtil)
				logger.info("Ecar generation done for Question: " + enrichedObj.identifier)

				saveOnSuccess(objWithEcar)(neo4JUtil, cassandraUtil, readerConfig, definitionCache, definitionConfig)
				logger.info("Question publishing completed successfully for : " + qData.identifier)
			} else {
				saveOnFailure(obj, messages)(neo4JUtil)
				logger.info("Question publishing failed for : " + qData.identifier)
			}
		})

	}
	def generateECAR(data: ObjectData, pkgTypes: List[String])(implicit ec: ExecutionContext, cloudStorageUtil: CloudStorageUtil): ObjectData = {
		logger.info("QuestionPublishFunction:generateECAR: Ecar generation done for Question: " + data.identifier)
		val ecarMap: Map[String, String] = generateEcar(data, pkgTypes)
		val variants: java.util.Map[String, String] = ecarMap.map { case (key, value) => key.toLowerCase -> value }.asJava
		logger.info("QuestionSetPublishFunction ::: generateECAR ::: ecar map ::: " + ecarMap)
		val meta: Map[String, AnyRef] = Map("downloadUrl" -> ecarMap.getOrElse("FULL", ""), "variants" -> variants)
		new ObjectData(data.identifier, data.metadata ++ meta, data.extData, data.hierarchy)
	}
}

class QuestionPublishUtil {}
