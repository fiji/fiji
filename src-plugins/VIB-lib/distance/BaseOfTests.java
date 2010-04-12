/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some very basic unit tests for the distance.MutualInformation class */

package distance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class BaseOfTests {

        /* This example corresponds to Exercise 8.6 on p140, with the
           following probabilities:

             P(x,y) |               x
                    |    1      2      3      4    |
             ---------------------------------------
                 1  |   1/8   1/16   1/32   1/32   |
              y  2  |  1/16    1/8   1/32   1/32   |
                 3  |  1/16   1/16   1/16   1/16   |
                 4  |   1/4      0      0      0   |
             ---------------------------------------
             
             This gives us the entropies:
     
                hx = 1.75
                hy = 2
                
                hxy = 3.375
     
                hxgiveny = 1.375
     
             So I(X;Y) is 0.375

	     (The R code for checking these is included in the comment at the bottom.)

        */

	public void addMacKayExample(PixelPairs measure) {		
		measure.add( 1, 1 );
		measure.add( 1, 1 );
		measure.add( 1, 1 );
		measure.add( 1, 1 );

		measure.add( 1, 2 );
		measure.add( 1, 2 );
        
		measure.add( 1, 3 );
		measure.add( 1, 3 );

		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );
		measure.add( 1, 4 );

		measure.add( 2, 1 );
		measure.add( 2, 1 );

		measure.add( 2, 2 );
		measure.add( 2, 2 );
		measure.add( 2, 2 );
		measure.add( 2, 2 );
		
		measure.add( 2, 3 );
		measure.add( 2, 3 );
		
		measure.add( 3, 1 );

		measure.add( 3, 2 );

		measure.add( 3, 3 );
		measure.add( 3, 3 );

		measure.add( 4, 1 );

		measure.add( 4, 2 );

		measure.add( 4, 3 );
		measure.add( 4, 3 );
	}

	/* Add uniform grid of values to the measure */

	public void addUniform8Bit(PixelPairs measure) {
		for(int i=0;i<256;++i)
			for(int j=0;j<256;++j)
				measure.add(i,j);
	}
}


/*

xv <- c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4)

yv <- c(1, 1, 1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4,
        1, 1, 2, 2, 2, 2, 3, 3, 1, 2, 3, 3, 1, 2, 3, 3)

diff <- xv - yv

euclideanDistance <- sqrt( sum( (diff * diff) / length(xv) ) )

x <- c(c(  1/8, 1/16, 1/32, 1/32 ),
       c( 1/16,  1/8, 1/32, 1/32 ),
       c( 1/16, 1/16, 1/16, 1/16 ),
       c(  1/4,    0,    0,    0 ) )

h <- function (p) {
  sum( p * log2( 1 / p ) )
}

m <- t(matrix( x, 4, 4 ))

nonzero <- x [ x > 0 ]

hxy <- h(nonzero)

px <- c( sum(m[,1]), sum(m[,2]), sum(m[,3]), sum(m[,4]) )
py <- c( sum(m[1,]), sum(m[2,]), sum(m[3,]), sum(m[4,]) )

hx <- h(px)

hy <- h(py)

pxgiveny <- c(m[1,] / py[1],
              m[2,] / py[2],
              m[3,] / py[3],
              m[4,] / py[4])

hxgiveny <- sum( as.numeric(x[x>0]) * log2(1/pxgiveny[x>0]) )

mi <- hx + hy - hxy

correlation <- cor(xv,yv)

 */