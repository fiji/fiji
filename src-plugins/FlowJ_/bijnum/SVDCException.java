package bijnum;

/**
*
*This is the exception produced by the Singular Value Decomposition method.
*See the SVDC_f77.java code for details.
*
*/
public class SVDCException extends Exception {

   public int info;


   public SVDCException(int info) {

      this.info = info;

   }

   public SVDCException(String problem) {

      super(problem);
   }

   public SVDCException() {

   }

}



