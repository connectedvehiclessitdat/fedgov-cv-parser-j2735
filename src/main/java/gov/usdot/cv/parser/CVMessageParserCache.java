package gov.usdot.cv.parser;

import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;

import java.util.HashMap;
import java.util.Map;

public class CVMessageParserCache {

	private static Map<Long, CVMessageParser> parserCache = new HashMap<Long, CVMessageParser>();

	static {
		parserCache.put(SemiDialogID.vehSitData.longValue(), 				new VehSitDataMessageParser());
		parserCache.put(SemiDialogID.dataSubscription.longValue(), 			new DataSubscriptionMessageParser());
		parserCache.put(SemiDialogID.advSitDataDep.longValue(), 			new TravelerInformationParser());
		parserCache.put(SemiDialogID.advSitDatDist.longValue(), 			new TravelerInformationQueryParser());
		parserCache.put(SemiDialogID.intersectionSitDataQuery.longValue(), 	new TravelerInformationQueryParser());
		parserCache.put(SemiDialogID.intersectionSitDataDep.longValue(), 	new IntersectionSitDataParser());
		parserCache.put(SemiDialogID.objReg.longValue(), 					new ObjectRegistrationDataParser());
		parserCache.put(SemiDialogID.objDisc.longValue(), 					new ObjectDiscoveryDataRequestParser());
	}

	public static CVMessageParser lookupParser(long dialogId) {
		return parserCache.get(dialogId);
	}
}
