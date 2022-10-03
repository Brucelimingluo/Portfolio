public class NBody {


	public static double readRadius(String file) {
		In in = new In(file);
		int number = in.readInt();
		double Radius = in.readDouble();
		return Radius;
	}

	public static Body[] readBodies(String file) { 
		In in = new In(file);
		int number = in.readInt();
		Body[] body = new Body[number];

		in.readDouble();

		for(int i = 0; i < number; i++) {
			double xP = in.readDouble();
			double yP = in.readDouble();
			double xV = in.readDouble();
			double yV = in.readDouble();
			double m = in.readDouble();
			String imgFileName = in.readString();
			body[i] = new Body(xP, yP, xV, yV, m, imgFileName);
		
		}
		return body;
	}

	public static void main(String[] args) {
		double T = Double.parseDouble(args[0]);
		double dt = Double.parseDouble(args[1]);
		String filename = args[2];
		Body[] bodies = NBody.readBodies(filename);
		double universeradius = NBody.readRadius(filename);

		StdDraw.setScale(-universeradius, universeradius);
		StdDraw.clear();
		StdDraw.picture(0, 0, "images/starfield.jpg");

		for (Body var: bodies) {
			var.draw();
		}

	///here

		StdDraw.enableDoubleBuffering();

		for ( double time = 0; time <= T; time += dt) {
			double[] xForces = new double[bodies.length];
			double[] yForces = new double[bodies.length];

			for (int i = 0; i < bodies.length; i++) {
				xForces[i] = bodies[i].calcNetForceExertedByX(bodies);
				yForces[i] = bodies[i].calcNetForceExertedByY(bodies);
		}

			for (int i = 0; i < bodies.length; i++) {
				bodies[i].update(dt, xForces[i], yForces[i]);
		}

			StdDraw.picture(0, 0, "images/starfield.jpg");

			for (int i = 0; i < bodies.length; i++) {
				bodies[i].draw();
		}

			StdDraw.show();

			StdDraw.pause(10);
		}

		StdOut.printf("%d\n", bodies.length);
		StdOut.printf("%.2e\n", universeradius);
		for (int i = 0; i < bodies.length; i++) {
   	 		StdOut.printf("%11.4e %11.4e %11.4e %11.4e %11.4e %12s\n",
                  bodies[i].xxPos, bodies[i].yyPos, bodies[i].xxVel,
                  bodies[i].yyVel, bodies[i].mass, bodies[i].imgFileName); 
   	 	}
}
	}


