package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import static gov.usdot.asn1.j2735.J2735Util.toFormattedDateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionCancel;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionRequest;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.SemiSequenceID;
import gov.usdot.asn1.generated.j2735.semi.VsmType;
import gov.usdot.asn1.j2735.CVSampleMessageBuilder;
import gov.usdot.cv.common.dialog.DataBundleUtil;
import gov.usdot.cv.common.model.Filter;
import gov.usdot.cv.resources.PrivateTestResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;

import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.deleidos.rtws.core.util.StandardHeader;
import com.deleidos.rtws.ext.parser.SimpleConfigurableTranslator;

public class DataSubscriptionRequestMessageParserTest {

	public static final String MODEL_NAME 			= "subscriptionMessage";
	public static final String MODEL_VERSION 		= "1.3";
	public static final String INPUT_FORMAT_NAME	= "subscriptionMessage";
	
	private static final String TEST_UUID = UUID.randomUUID().toString();
	private static final String DEST_HOST = "2607:f0d0:1002:0051:0000:0000:0000:0004";
	private static final int DEST_PORT = 47651;
	private static final boolean FROM_FORWARDER = true;
	private static final String CERTIFICATE = "Some Certificate Text";
	
	private static final byte [] reqID = new byte[] {(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01};
	
	private J2735Parser parser;
	private SimpleConfigurableTranslator translator;
	
	private byte [] subscriptionEncodedBytes;
	private DataSubscriptionRequest subscriptionMsg;
	
	private byte [] cancelEncodedBytes;
	private DataSubscriptionCancel cancelMsg;

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
				PrivateTestResourceLoader.getProperty("@parser-j2735/data.subscription.request.message.parser.test.password@"));
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

		subscriptionMsg = CVSampleMessageBuilder.buildDataSubscriptionRequest();
		subscriptionEncodedBytes = CVSampleMessageBuilder.messageToEncodedBytes(subscriptionMsg);
		
		cancelMsg = CVSampleMessageBuilder.buildDataSubscriptionCancel();
		cancelEncodedBytes = CVSampleMessageBuilder.messageToEncodedBytes(cancelMsg);
	}

	@Test
	public void testInitialize() {
		parser.initialize();
	}

	@Test
	public void testGoodSubscriptionRequest() throws Exception {
		String internalDataBundle = DataBundleUtil.encode(
				TEST_UUID.getBytes(), 
				DEST_HOST.getBytes(), 
				DEST_PORT, 
				FROM_FORWARDER,
				CERTIFICATE.getBytes(), 
				subscriptionEncodedBytes);
		
		parser.initialize();
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));

		JSONObject jsonObj;
		while ((jsonObj = parser.parse()) != null) {
			JSONObject headerObj = jsonObj.getJSONObject("standardHeader");
			assertNotNull(headerObj);
			assertTrue(headerObj.getString(StandardHeader.ACCESS_LABEL_KEY).equals("UNCLASSIFIED"));
			assertTrue(headerObj.getString(StandardHeader.SOURCE_KEY).equals("JUNIT1"));

			assertEquals(SemiDialogID.dataSubscription.longValue(), jsonObj.getLong(DataSubscriptionMessageParser.DIALOG_ID));
			assertEquals(SemiSequenceID.subscriptionReq.longValue(), jsonObj.getLong(DataSubscriptionMessageParser.SEQUENCE_ID));
			assertEquals(ByteBuffer.wrap(reqID).getInt(), jsonObj.getInt(DataSubscriptionMessageParser.REQUEST_ID));
			assertEquals(0, jsonObj.getLong(DataSubscriptionMessageParser.GROUP_ID));
			assertNotNull(jsonObj.getString(DataSubscriptionMessageParser.CERTIFICATE));
			assertNotNull(jsonObj.getString(DataSubscriptionMessageParser.DEST_HOST));
			assertNotNull(jsonObj.getInt(DataSubscriptionMessageParser.DEST_PORT));
			assertNotNull(jsonObj.getString(DataSubscriptionMessageParser.FROM_FORWARDER));
			
			assertEquals(toFormattedDateString(subscriptionMsg.getEndTime(), Filter.DATE_PATTERN), jsonObj.getString(DataSubscriptionMessageParser.END_TIME));
			assertTrue(subscriptionMsg.getType().hasVsmType());
			
			VsmType vsmType = (VsmType) subscriptionMsg.getType().getChosenValue();
			assertEquals(ByteBuffer.wrap(vsmType.byteArrayValue()).get(), jsonObj.getInt(DataSubscriptionMessageParser.TYPE_VALUE));

			assertEquals(convertGeoCoordinateToDouble(
					subscriptionMsg.getServiceRegion().getNwCorner().getLat().intValue()),
					jsonObj.getJSONObject("nwPos").getDouble("lat"), .001);
			assertEquals(convertGeoCoordinateToDouble(
					subscriptionMsg.getServiceRegion().getNwCorner().get_long().intValue()),
					jsonObj.getJSONObject("nwPos").getDouble("lon"), .001);

			assertEquals(convertGeoCoordinateToDouble(
					subscriptionMsg.getServiceRegion().getSeCorner().getLat().intValue()),
					jsonObj.getJSONObject("sePos").getDouble("lat"), .001);
			assertEquals(convertGeoCoordinateToDouble(
					subscriptionMsg.getServiceRegion().getSeCorner().get_long().intValue()),
					jsonObj.getJSONObject("sePos").getDouble("lon"), .001);
		}
	}
	
	@Test
	public void testGoodCancelRequest() throws Exception {
		String internalDataBundle = DataBundleUtil.encode(
				TEST_UUID.getBytes(), 
				DEST_HOST.getBytes(), 
				DEST_PORT, 
				FROM_FORWARDER,
				CERTIFICATE.getBytes(), 
				cancelEncodedBytes);
		
		parser.initialize();
		parser.setInputStream(new ByteArrayInputStream(internalDataBundle.getBytes()));

		JSONObject jsonObj;
		while ((jsonObj = parser.parse()) != null) {
			JSONObject headerObj = jsonObj.getJSONObject("standardHeader");
			assertNotNull(headerObj);
			assertTrue(headerObj.getString(StandardHeader.ACCESS_LABEL_KEY).equals("UNCLASSIFIED"));
			assertTrue(headerObj.getString(StandardHeader.SOURCE_KEY).equals("JUNIT1"));

			assertEquals(SemiDialogID.dataSubscription.longValue(), jsonObj.getLong(DataSubscriptionMessageParser.DIALOG_ID));
			assertEquals(SemiSequenceID.subscriptionCancel.longValue(), jsonObj.getLong(DataSubscriptionMessageParser.SEQUENCE_ID));
			assertEquals(0, jsonObj.getLong(DataSubscriptionMessageParser.GROUP_ID));
			assertEquals(ByteBuffer.wrap(reqID).getInt(), jsonObj.getInt(DataSubscriptionMessageParser.REQUEST_ID));
			assertEquals(10000004, jsonObj.getInt(DataSubscriptionMessageParser.SUBSCRIBER_ID));
		}
	}

}
