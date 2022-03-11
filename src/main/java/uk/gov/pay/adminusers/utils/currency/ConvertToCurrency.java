package uk.gov.pay.adminusers.utils.currency;

import java.math.BigDecimal;
import java.util.function.Function;

public class ConvertToCurrency {

   public static Function<Long, BigDecimal> convertPenceToPounds = pence -> new BigDecimal(pence).movePointLeft(2); 
}
