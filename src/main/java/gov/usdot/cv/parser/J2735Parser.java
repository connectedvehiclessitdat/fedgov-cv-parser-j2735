package gov.usdot.cv.parser;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.generated.j2735.semi.AdvisorySituationData;
import gov.usdot.asn1.generated.j2735.semi.DataRequest;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionCancel;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionRequest;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.asn1.generated.j2735.semi.ObjectDiscoveryDataRequest;
import gov.usdot.asn1.generated.j2735.semi.ObjectRegistrationData;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.dialog.DataBundle;
import gov.usdot.cv.common.dialog.DataBundleUtil;

import java.text.ParseException;
import java.util.Map;

import javax.validation.constraints.NotNull;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.deleidos.rtws.commons.exception.InitializationException;
import com.deleidos.rtws.commons.util.Initializable;
import com.deleidos.rtws.core.framework.Description;
import com.deleidos.rtws.core.framework.UserConfigured;
import com.deleidos.rtws.core.framework.parser.AbstractLineParser;
import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.deleidos.rtws.core.framework.translator.AbstractConfigurableTranslator;
import com.deleidos.rtws.core.util.StandardHeader;
import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.ControlTableNotFoundException;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;

@Description("Parses messages encoded in the ASN.1 J2375 spec")
public class J2735Parser extends AbstractLineParser implements Initializable {

	private static final Logger logger = Logger.getLogger(J2735Parser.class);
	
	protected AbstractConfigurableTranslator translator;
	
	protected String defaultSource;
	
	protected String defaultAccessLabel;
	
	protected boolean enableBERDebugging = false;
	
	protected boolean decodeBundle = false;
	
	private Coder coder;
	
	public J2735Parser() {
		super("UTF-8");
	}
	
	public void initialize() throws InitializationException {
		translator.initialize();
		initializeCoder();
	}
	
	void initializeCoder() throws InitializationException {
		try {
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
			if (enableBERDebugging) {
				coder.enableEncoderDebugging();
				coder.enableDecoderDebugging();
			}
		} catch (ControlTableNotFoundException e) {
			logger.error(e);
			throw new InitializationException("J2735Parser initialization failure", e);
		} catch (com.oss.asn1.InitializationException e) {
			logger.error(e);
			throw new InitializationException("J2735Parser initialization failure", e);
		}
	}

	public void dispose() {
		translator.dispose();
		J2735.deinitialize();
	}
	
	public void parseHeaders() {
		// no Headers
	}

	public JSONObject parse() throws ParsePipelineException {
		String input = nextRecord();
		if (input == null) return null;
		
		String streamAccessLabel = info.getProperty(StandardHeader.ACCESS_LABEL_KEY);
		String accessLabel = (streamAccessLabel == null) ? defaultAccessLabel : streamAccessLabel;
		
		String streamSource = info.getProperty(StandardHeader.SOURCE_KEY);
		String source = (streamSource == null) ? defaultSource : streamSource;
		
		DataBundle bundle = null;
		AbstractData message = null;
		Map<String, String> map = null;
		
		try {
			bundle = DataBundleUtil.decode(input);
			message = decodeBER(bundle.getPayload());
		} catch (Exception ex) {
			logger.error("Failed to decode and unwrap payload.", ex);
			return null;
		}
		
		Long dialogId = getDialogId(message);
		if (dialogId == null) {
			logger.error("No dialog id found for message: " + message);
			return null;
		}
		
		CVMessageParser parser = CVMessageParserCache.lookupParser(dialogId);
		if (parser == null) {
			logger.error("No CVMessageParser found for dialog id: " + dialogId);
			return null;
		}
		
		try {
			map = parser.parse(message, bundle);
			return translator.recordTranslation(map, source, accessLabel);
		} catch (ParseException pe) {
			throw new ParsePipelineException("Failed to translate the message map to input data model.", pe);
		}
	}
	
	private Long getDialogId(AbstractData message) {
		if (message instanceof VehSitDataMessage) {
			return ((VehSitDataMessage) message).getDialogID().longValue();
		} else if (message instanceof DataSubscriptionRequest) {
			return ((DataSubscriptionRequest) message).getDialogID().longValue();
		} else if (message instanceof DataSubscriptionCancel) {
			return ((DataSubscriptionCancel) message).getDialogID().longValue();
		} else if (message instanceof AdvisorySituationData) {
			return ((AdvisorySituationData) message).getDialogID().longValue();
		} else if (message instanceof DataRequest) {
			return ((DataRequest) message).getDialogID().longValue();
		} else if (message instanceof IntersectionSituationData) {
			return ((IntersectionSituationData) message).getDialogID().longValue();  
		} else if (message instanceof ObjectRegistrationData) {
			return ((ObjectRegistrationData) message).getDialogID().longValue();  
		} else if (message instanceof ObjectDiscoveryDataRequest) {
			return ((ObjectDiscoveryDataRequest) message).getDialogID().longValue();  
		} else {
			return null;
		}
	}
	
	private AbstractData decodeBER(byte [] message) throws ParsePipelineException {
		try {
			return J2735Util.decode(coder, message);
		} catch (DecodeFailedException e) {
			String errorMessage = String.format("BER decode failed with Exception: %s", e.toString());
			logger.error(errorMessage);
			throw new ParsePipelineException(errorMessage, e);
		} catch (DecodeNotSupportedException e) {
			String errorMessage = String.format("BER decode failed with Exception: %s", e.toString());
			logger.error(errorMessage);
			throw new ParsePipelineException(errorMessage, e);
		}
	}
	
	/**
	 * Set the translator that will convert the parsed fields to a data model.
	 */
	public void setTranslator(AbstractConfigurableTranslator translator) {
		this.translator = translator;
	}
	
	/**
	 * @return the translator
	 */
	@NotNull
	public AbstractConfigurableTranslator getTranslator() {
		return this.translator;
	}
	
	/**
	 * Set the default source string of the data. Can be overridden by the input stream parameters.
	 */
	@UserConfigured(value="UNKNOWN", description = "The string describing the source of the data.")
	public void setDefaultSource(String defaultSource) {
		this.defaultSource = defaultSource;
	}
	
	/**
	 * @return the default source
	 */
	@NotNull
	public String getDefaultSource() {
		return this.defaultSource;
	}

	/**
	 * Set the default access label. Can be overridden by the input stream parameters or
	 * the translator.
	 */
	@UserConfigured(value="UNCLASSIFIED", description = "The default access label to include with the data.")
	public void setDefaultAccessLabel(String defaultAccessLabel) {
		this.defaultAccessLabel = defaultAccessLabel;
	}
	
	/**
	 * @return the default access label
	 */
	@NotNull
	public String getDefaultAccessLabel() {
		return this.defaultAccessLabel;
	}

	public boolean isEnableBERDebugging() {
		return enableBERDebugging;
	}

	@UserConfigured(value="false",
			flexValidator = { "RegExpValidator expression=true|false" },
			description="Turn on debugging log output for the BER Decoder")
	public void setEnableBERDebugging(boolean enableBERDebugging) {
		this.enableBERDebugging = enableBERDebugging;
	}

	public boolean isDecodeBundle() {
		return decodeBundle;
	}

	@UserConfigured(value="false",
			flexValidator = { "RegExpValidator expression=true|false" },
			description="Decode the VehSitRcdBundle portion of the VehSitDataMessage")
	public void setDecodeBundle(boolean decodeBundle) {
		this.decodeBundle = decodeBundle;
	}
	
}