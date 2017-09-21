package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.rtws.core.util.StandardHeader;
import com.deleidos.rtws.ext.parser.SimpleConfigurableTranslator;
import com.oss.asn1.Coder;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.generated.j2735.dsrc.DDateTime;
import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.dsrc.TemporaryID;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.SemiSequenceID;
import gov.usdot.asn1.generated.j2735.semi.ServiceRequest;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage.Bundle;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.j2735.CVSampleMessageBuilder;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundleUtil;
import gov.usdot.cv.resources.PrivateTestResourceLoader;
import net.sf.json.JSONObject;

public class VehSitDataMessageParserTest {

	public static final String MODEL_NAME 			= "vehSitDataMessage";
	public static final String MODEL_VERSION 		= "1.6";
	public static final String INPUT_FORMAT_NAME	= "vsdm";
	
	private static final String TEST_UUID = UUID.randomUUID().toString();
	private static final String DEST_HOST = "2607:f0d0:1002:0051:0000:0000:0000:0004";
	private static final int DEST_PORT = 47651;
	private static final boolean FROM_FORWARDER = true;
	private static final String CERTIFICATE = "Some Certificate Text";
	
	private J2735Parser parser;
	private SimpleConfigurableTranslator translator;
	private byte[] encodedBytes;
	private VehSitDataMessage msg;

	@BeforeClass
	public static void init() {
		Properties testProperties = System.getProperties();
		if (testProperties.getProperty("RTWS_CONFIG_DIR") == null) {
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
				PrivateTestResourceLoader.getProperty("@parser-j2735/veh.sit.data.message.parser.test.password@"));
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
		
		msg = CVSampleMessageBuilder.buildVehSitDataMessage();
		encodedBytes = CVSampleMessageBuilder.messageToEncodedBytes(msg);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInitialize() {
		parser.initialize();
	}

	@Test
	public void testGoodParse() throws Exception {
		String internalDataBundle = DataBundleUtil.encode(
			TEST_UUID.getBytes(), 
			DEST_HOST.getBytes(), 
			DEST_PORT, 
			FROM_FORWARDER,
			CERTIFICATE.getBytes(), 
			encodedBytes);

		parser.initialize();
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));

		JSONObject jsonObj;
		while ((jsonObj = parser.parse()) != null) {
			System.out.println(jsonObj.toString(4));
			JSONObject headerObj = jsonObj.getJSONObject("standardHeader");
			assertNotNull(headerObj);
			assertTrue(headerObj.getString(StandardHeader.ACCESS_LABEL_KEY)
					.equals("UNCLASSIFIED"));
			assertTrue(headerObj.getString(StandardHeader.SOURCE_KEY).equals(
					"JUNIT1"));
			
			assertEquals(SemiDialogID.vehSitData.longValue(), jsonObj.getLong("dialogId"));
			assertEquals(SemiSequenceID.data.longValue(), jsonObj.getLong("sequenceId"));
			assertEquals(0, jsonObj.getLong("groupId"));
			assertNotNull(jsonObj.getLong("requestId"));
			assertEquals(ByteBuffer.wrap(msg.getType().byteArrayValue()).get(), jsonObj.getInt("vsmType"));
			
			Bundle bundle = msg.getBundle();
			assertNotNull(bundle);
			int count = bundle.getSize();
			assertTrue(count >  0);
			VehSitRecord vsr = bundle.get(0);
			assertNotNull(vsr);
			verifyVehSitRecord(jsonObj, vsr);
			assertEquals(count, jsonObj.getInt("count"));
		}
	}
	
	private static void verifyVehSitRecord(JSONObject jsonObj, VehSitRecord vsr){
		DDateTime dateTime = vsr.getTime();
		
		assertEquals(dateTime.getYear().intValue(), jsonObj.getInt("year"));
		assertEquals(dateTime.getMonth().intValue(), jsonObj.getInt("month"));
		assertEquals(dateTime.getDay().intValue(), jsonObj.getInt("day"));
		assertEquals(dateTime.getHour().intValue(), jsonObj.getInt("hour"));
		assertEquals(dateTime.getMinute().intValue(), jsonObj.getInt("minute"));
		assertEquals(dateTime.getSecond().intValue(), jsonObj.getInt("second"));
		
		Position3D pos = vsr.getPos();
		assertEquals(convertGeoCoordinateToDouble(pos.get_long().intValue()), jsonObj.getDouble("long"), .001);
		assertEquals(convertGeoCoordinateToDouble(pos.getLat().intValue()), jsonObj.getDouble("lat"), .001);
		assertEquals(pos.getElevation().intValue(), jsonObj.getInt("elevation"));
	}
	
	@Test
	public void testNonVehSitDataMessage() throws Exception {
		parser.initialize();
		
		Coder coder = J2735.getPERUnalignedCoder();
		ServiceRequest vsr = new ServiceRequest();
		vsr.setDialogID(SemiDialogID.vehSitData);
		vsr.setSeqID(SemiSequenceID.svcReq);
		vsr.setGroupID(GroupIDHelper.toGroupID(0));
		vsr.setRequestID(new TemporaryID(ByteBuffer.allocate(4).putInt(1001).array()));
		
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		coder.encode(vsr, sink);
		byte[] encoding = sink.toByteArray();
		
		String internalDataBundle = DataBundleUtil.encode(
				TEST_UUID.getBytes(), 
				DEST_HOST.getBytes(), 
				DEST_PORT, 
				FROM_FORWARDER,
				CERTIFICATE.getBytes(), 
				encoding);
		
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));
		JSONObject obj = parser.parse();
		assertNull(obj);
	}
}
