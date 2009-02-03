package jRenderer3D;
class Misc {
		
		static String fm(int len, int val) {
			String s = "" + val;
			
			while (s.length() < len) {
				s = " " + s;
			}
			return s;
		}
		static String fm(int len, double val) {
			String s = "" + val;
			
			while (s.length() < len) {
				s = s + " ";
			}
			return s;
		}
		
		static boolean inside(int[] p, int[] p1, int[] p2, int[] p3) {
			int x  = p[0];
			int y  = p[1];
			int x1 = p1[0];
			int y1 = p1[1];
			int x2 = p2[0];
			int y2 = p2[1];
			int x3 = p3[0];
			int y3 = p3[1];
			
			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
			
			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}
		
		static boolean inside(int x, int y, int x1, int y1, int x2, int y2, int x3, int y3) {
			
			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
			
			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}
		
	}