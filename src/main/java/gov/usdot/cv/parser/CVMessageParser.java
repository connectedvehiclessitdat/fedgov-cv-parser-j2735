package gov.usdot.cv.parser;

import gov.usdot.cv.common.dialog.DataBundle;

import java.util.Map;

import com.oss.asn1.AbstractData;
import com.deleidos.rtws.core.framework.parser.ParsePipelineException;

public interface CVMessageParser {
	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle) throws ParsePipelineException;
}
