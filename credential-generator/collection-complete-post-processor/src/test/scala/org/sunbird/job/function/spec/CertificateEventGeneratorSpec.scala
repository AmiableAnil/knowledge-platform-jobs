package org.sunbird.job.function.spec

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import com.google.gson.Gson
import com.typesafe.config.{Config, ConfigFactory}
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.cql.FileCQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.mockito.ArgumentMatchers.{any, endsWith}
import org.sunbird.job.Metrics
import org.mockito.Mockito.when
import org.sunbird.job.cache.{DataCache, RedisConnect}
import org.sunbird.job.fixture.EventFixture
import org.sunbird.job.functions.{CertificateApiService, CertificateEventGenerator}
import org.sunbird.job.task.CollectionCompletePostProcessorConfig
import org.sunbird.job.util.{CassandraUtil, HTTPResponse, HttpUtil}
import org.sunbird.spec.{BaseMetricsReporter, BaseTestSpec}
import redis.embedded.RedisServer

class CertificateEventGeneratorSpec extends BaseTestSpec {

  val config: Config = ConfigFactory.load("test.conf")
  val jobConfig = new CollectionCompletePostProcessorConfig(config)
  val mockHttpUtil = mock[HttpUtil]
  val metrics = Metrics(new ConcurrentHashMap[String, AtomicLong]() {
    {
      put(jobConfig.dbReadCount, new AtomicLong())
      put(jobConfig.cacheReadCount, new AtomicLong())
    }
  })
  var cassandraUtil: CassandraUtil = _
  var redisServer: RedisServer = _
  val redisConnect = new RedisConnect(jobConfig)
  lazy private val gson = new Gson()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    redisServer = new RedisServer(6340)
    redisServer.start()
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(80000L)
    cassandraUtil = new CassandraUtil(jobConfig.dbHost, jobConfig.dbPort)
    val session = cassandraUtil.session
    val dataLoader = new CQLDataLoader(session);
    dataLoader.load(new FileCQLDataSet(getClass.getResource("/test.cql").getPath, true, true));
    testCassandraUtil(cassandraUtil)
    BaseMetricsReporter.gaugeMetrics.clear()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    try {
      redisServer.stop()
      EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
    } catch {
      case ex: Exception => {
      }
    }
  }

  //test should generate output event edata
  it should "prepareGenerateEventEdata" in {
    mockAll()
    val dataCache = new DataCache(jobConfig, redisConnect, 2, List())
    dataCache.init()
    val edata = gson.fromJson(EventFixture.REQUEST_EVENT, classOf[java.util.Map[String, AnyRef]])
    val eventData = new CertificateEventGenerator(jobConfig)(metrics, cassandraUtil).prepareGenerateEventEdata(edata, dataCache)
    assert(null != eventData)
    assert(eventData.containsKey("issuer"))
    assert(eventData.containsKey("svgTemplate"))
    dataCache.close()
  }

  it should "prepareGenerateEventEdata with reIssue false" in {
    mockAll()
    val dataCache = new DataCache(jobConfig, redisConnect, 2, List())
    dataCache.init()
    val edata = gson.fromJson(EventFixture.REQUEST_EVENT_4, classOf[java.util.Map[String, AnyRef]])
    val eventData = new CertificateEventGenerator(jobConfig)(metrics, cassandraUtil).prepareGenerateEventEdata(edata, dataCache)
    assert(null != eventData)
    assert(eventData.containsKey("issuer"))
    assert(eventData.containsKey("svgTemplate"))
    dataCache.close()
  }

  it should "thow exception for prepareGenerateEventEdata with reIssue false and issued certificate exist" in intercept[Exception] {
    mockAll()
    val dataCache = new DataCache(jobConfig, redisConnect, 2, List())
    dataCache.init()
    val edata = gson.fromJson(EventFixture.REQUEST_EVENT_2, classOf[java.util.Map[String, AnyRef]])
    new CertificateEventGenerator(jobConfig)(metrics, cassandraUtil).prepareGenerateEventEdata(edata, dataCache)
    dataCache.close()
  }

  it should "prepareGenerateEventEdata with reIssue false and empty issuedCertificate" in {
    mockAll()
    val dataCache = new DataCache(jobConfig, redisConnect, 2, List())
    dataCache.init()
    val edata = gson.fromJson(EventFixture.REQUEST_EVENT_3, classOf[java.util.Map[String, AnyRef]])
    val eventData = new CertificateEventGenerator(jobConfig)(metrics, cassandraUtil).prepareGenerateEventEdata(edata, dataCache)
    assert(null != eventData)
    assert(eventData.containsKey("issuer"))
    assert(eventData.containsKey("svgTemplate"))
    //    assert(gson.toJson(edata).equals(EventFixture.FINAL_EDATA_3))
    dataCache.close()
  }

  private def mockAll(): Unit = {
    CertificateApiService.httpUtil = mockHttpUtil
    when(mockHttpUtil.post(endsWith("/v1/search"), any[String])).thenReturn(HTTPResponse(200, """{"id":"","ver":"private","ts":"2020-10-28 07:31:36:637+0000","params":{"resmsgid":null,"msgid":"8e27cbf5-e299-43b0-bca7-8347f7e5abcf","err":null,"status":"success","errmsg":null},"responseCode":"OK","result":{"response":{"count":1,"content":[{"firstName":"Reviewer","lastName":"User","maskedPhone":"******7418","rootOrgName":"Sunbird","userName":"ntptest103","rootOrgId":"ORG_001"}]}}}""".stripMargin))
    when(mockHttpUtil.get(endsWith("/v3/read/do_11309999837886054415"))).thenReturn(HTTPResponse(200, """{ "id": "api.v3.read", "ver": "1.0", "ts": "2020-10-24T15:25:39.187Z", "params": { "resmsgid": "2be6e430-160d-11eb-98c2-3bbec8c9cf05", "msgid": "2be4e860-160d-11eb-98c2-3bbec8c9cf05", "status": "successful", "err": null, "errmsg": null }, "responseCode": "OK", "result": {"content": {"name": "Test-audit-svg-7-oct-2", "status": "Live", "code": "org.sunbird.4SZ9XP"}}}""".stripMargin))
    when(mockHttpUtil.get(endsWith("/v3/read/course_003"))).thenReturn(HTTPResponse(200, """{ "id": "api.v3.read", "ver": "1.0", "ts": "2020-10-24T15:25:39.187Z", "params": { "resmsgid": "2be6e430-160d-11eb-98c2-3bbec8c9cf05", "msgid": "2be4e860-160d-11eb-98c2-3bbec8c9cf05", "status": "successful", "err": null, "errmsg": null }, "responseCode": "OK", "result": {"content": {"name": "Test-audit-svg-7-oct-2", "status": "Live", "code": "org.sunbird.4SZ9XP"}}}""".stripMargin))
    when(mockHttpUtil.post(endsWith("/v1/org/read"), any[String])).thenReturn(HTTPResponse(200, """{ "id": "api.org.read", "ver": "v1", "ts": "2020-10-24 14:47:23:631+0000", "params": { "resmsgid": null, "msgid": "cc58e03e2789f6db8b4695a43a5c8a39", "err": null, "status": "success", "errmsg": null }, "responseCode": "OK", "result": {"keys": {"signKeys": [{"testKey": "testValue"}]}}}""".stripMargin))
  }

  private def testCassandraUtil(cassandraUtil: CassandraUtil): Unit = {
    cassandraUtil.reconnect()
  }

}
