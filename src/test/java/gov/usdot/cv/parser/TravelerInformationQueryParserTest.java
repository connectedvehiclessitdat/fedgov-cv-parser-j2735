package gov.usdot.cv.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.usdot.asn1.generated.j2735.semi.DataRequest;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.SemiSequenceID;
import gov.usdot.asn1.j2735.IntersectionSitDataBuilder;
import gov.usdot.asn1.j2735.TravelerSampleMessageBuilder;
import gov.usdot.cv.common.dialog.DataBundleUtil;
import gov.usdot.cv.resources.PrivateTestResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.rtws.core.util.StandardHeader;
import com.deleidos.rtws.ext.parser.SimpleConfigurableTranslator;

public class TravelerInformationQueryParserTest {

	public static final String MODEL_NAME 			= "queryMessage";
	public static final String MODEL_VERSION 		= "1.3";
	public static final String INPUT_FORMAT_NAME	= "queryMessage";
	
	private static final String TEST_UUID = UUID.randomUUID().toString();
	private static final String DEST_HOST = "2607:f0d0:1002:0051:0000:0000:0000:0004";
	private static final int DEST_PORT = 47651;
	private static final boolean FROM_FORWARDER = true;
	private static final String CERTIFICATE = "Some Certificate Text";
	
	private J2735Parser parser;
	private SimpleConfigurableTranslator translator;
	private byte [] encodedAdvSitDataBytes;
	private byte [] encodedIntersectionSitDataBytes;

	@BeforeClass
	public static void init() {
		Properties testProperties = System.getProperties();
		if (testProperties.getProperty("RTWS_CONFIG_DIR") == null ) {
			testProperties.setProperty("RTWS_CONFIG_DIR", testProperties.getProperty("basedir", "."));
			try {
				testProperties.load(
						PrivateTestResourceLoader.getFileAsStream(
								"@properties/parser-j2735-filtering.properties@"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		testProperties.setProperty("RTWS_TEST_MODE", "true");
		testProperties.setProperty("RTWS_TENANT_ID", "aws-dev");
		testProperties.setProperty("password",
				PrivateTestResourceLoader.getProperty("@parser-j2735/traveler.information.query.parser.test.password@"));
		testProperties.setProperty("RTWS_BUCKET_NAME", "test-bucket");
		testProperties.setProperty("RTWS_DOMAIN", "nothing.rtsaic.com");
		testProperties.setProperty("RTWS_MOUNT_MODE", "s3cmd");
		testProperties.setProperty("RTWS_MAX_ALLOCATION_REQUEST", "");
		System.setProperties(testProperties);
	}

	@Before
	public void setUp() throws Exception {
		parser = new J2735Parser();
		parser.setEnableBERDebugging(false);
		parser.setDecodeBundle(true);
		parser.setDefaultSource("JUNIT1");
		parser.setDefaultAccessLabel("UNCLASSIFIED");
		Properties emptyProps = new Properties();
		parser.setStreamProperties(emptyProps);
		translator = new SimpleConfigurableTranslator();
		translator.setModelName(MODEL_NAME);
		translator.setModelVersion(MODEL_VERSION);
		translator.setInputFormatName(INPUT_FORMAT_NAME);
		parser.setTranslator(translator);

		DataRequest rsuDataRequestMsg = TravelerSampleMessageBuilder.buildRsuAdvisorySituationDataRequest();
		encodedAdvSitDataBytes = TravelerSampleMessageBuilder.messageToEncodedBytes(rsuDataRequestMsg);
		
		DataRequest intersectionDataRequestMsg = IntersectionSitDataBuilder.buildIntersectionSituationDataRequest();
		encodedIntersectionSitDataBytes = IntersectionSitDataBuilder.messageToEncodedBytes(intersectionDataRequestMsg);
	}

	@Test
	public void testInitialize() {
		parser.initialize();
	}

	@Test
	public void testGoodRsuAdvisorySituationDataRequestParse() throws Exception {
		String internalDataBundle = DataBundleUtil.encode(
				TEST_UUID.getBytes(), 
				DEST_HOST.getBytes(), 
				DEST_PORT, 
				FROM_FORWARDER,
				CERTIFICATE.getBytes(), 
				encodedAdvSitDataBytes);
		
		parser.initialize();
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));

		JSONObject jsonObj;
		while ((jsonObj = parser.parse()) != null) {
			JSONObject headerObj = jsonObj.getJSONObject("standardHeader");
			assertNotNull(headerObj);
			assertTrue(headerObj.getString(StandardHeader.ACCESS_LABEL_KEY).equals("UNCLASSIFIED"));
			assertTrue(headerObj.getString(StandardHeader.SOURCE_KEY).equals("JUNIT1"));

			assertEquals(SemiDialogID.advSitDatDist.longValue(), jsonObj.getLong("dialogId"));
			assertEquals(SemiSequenceID.dataReq.longValue(), jsonObj.getLong("sequenceId"));
			assertEquals(0, jsonObj.getLong(TravelerInformationQueryParser.GROUP_ID));
			assertEquals(TEST_UUID, jsonObj.getString("receiptId"));
			assertNotNull(jsonObj.getLong(TravelerInformationQueryParser.REQUEST_ID));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.DEST_HOST));
			assertNotNull(jsonObj.getInt(TravelerInformationQueryParser.DEST_PORT));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.FROM_FORWARDER));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.CERTIFICATE));
			assertNotNull(jsonObj.getJSONObject("nwPos").getLong("lat"));
			assertNotNull(jsonObj.getJSONObject("nwPos").getLong("lon"));
			assertNotNull(jsonObj.getJSONObject("sePos").getLong("lat"));
			assertNotNull(jsonObj.getJSONObject("sePos").getLong("lon"));
		}
	}
	
	@Test
	public void testGoodIntersectionSituationDataRequestParse() throws Exception {
		String internalDataBundle = DataBundleUtil.encode(
				TEST_UUID.getBytes(), 
				DEST_HOST.getBytes(), 
				DEST_PORT, 
				FROM_FORWARDER,
				CERTIFICATE.getBytes(), 
				encodedIntersectionSitDataBytes);
		
		parser.initialize();
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));

		JSONObject jsonObj;
		while ((jsonObj = parser.parse()) != null) {
			JSONObject headerObj = jsonObj.getJSONObject("standardHeader");
			assertNotNull(headerObj);
			assertTrue(headerObj.getString(StandardHeader.ACCESS_LABEL_KEY).equals("UNCLASSIFIED"));
			assertTrue(headerObj.getString(StandardHeader.SOURCE_KEY).equals("JUNIT1"));

			assertEquals(SemiDialogID.intersectionSitDataQuery.longValue(), jsonObj.getLong("dialogId"));
			assertEquals(SemiSequenceID.dataReq.longValue(), jsonObj.getLong("sequenceId"));
			assertEquals(TEST_UUID, jsonObj.getString("receiptId"));
			assertNotNull(jsonObj.getLong(TravelerInformationQueryParser.REQUEST_ID));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.DEST_HOST));
			assertNotNull(jsonObj.getInt(TravelerInformationQueryParser.DEST_PORT));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.FROM_FORWARDER));
			assertNotNull(jsonObj.getString(TravelerInformationQueryParser.CERTIFICATE));
			assertNotNull(jsonObj.getJSONObject("nwPos").getLong("lat"));
			assertNotNull(jsonObj.getJSONObject("nwPos").getLong("lon"));
			assertNotNull(jsonObj.getJSONObject("sePos").getLong("lat"));
			assertNotNull(jsonObj.getJSONObject("sePos").getLong("lon"));
			assertNotNull(jsonObj.getInt(TravelerInformationQueryParser.TIME_BOUND));
			assertEquals(jsonObj.getInt(TravelerInformationQueryParser.TIME_BOUND), 15);
		}
	}

}
