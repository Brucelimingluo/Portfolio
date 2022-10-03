public class Body {
	public double xxPos;
	public double yyPos;
	public double xxVel;
	public double yyVel;
    public double mass;
    public String imgFileName;



	public Body(double xP, double yP, double xV,
              double yV, double m, String img) {
		xxPos = xP;
		yyPos = yP;
		xxVel = xV;
		yyVel = yV;
		mass = m;
		imgFileName = img;

	}

	public Body(Body b) {
    	xxPos = b.xxPos;
    	yyPos = b.yyPos;
    	xxVel = b.xxVel;
    	yyVel = b.yyVel;
    	mass = b.mass;
    	imgFileName = b.imgFileName;

    }

    public double calcDistance(Body b) {
    	double dx = this.xxPos - b.xxPos;
    	double dy = this.yyPos - b.yyPos;
    	double r = Math.hypot(dx, dy);
    	return r;
    }

    public double calcForceExertedBy(Body b) {
    	double G = 6.67e-11;
    	double R = calcDistance(b);
    	double F = G * this.mass * b.mass / (R * R);
    	return F;

    }

    public double calcForceExertedByX(Body b) {
    	double fx = this.calcForceExertedBy(b) * (b.xxPos - this.xxPos) / this.calcDistance(b);
    	return fx;
    }

    public double calcForceExertedByY(Body b) {
    	double fy = this.calcForceExertedBy(b) * (b.yyPos - this.yyPos) / this.calcDistance(b);
    	return fy;
    }

    public double calcNetForceExertedByX(Body[] B) {
    	double Netx = 0;
    	for (Body P : B) {
    		if (this.equals(P) == false) {
    			Netx += this.calcForceExertedByX(P);
    		}
    	}
    	return Netx;
    }
    


	public double calcNetForceExertedByY(Body[] B) {
    	double Nety = 0;
    	for (Body b : B) {
    		if (this.equals(b) == false) {
    			Nety += this.calcForceExertedByY(b);
    		}
    	}
    	return Nety;
    }
    

    public void update(double dt, double fX, double fY) {
    	double aX = fX / this.mass;
    	double aY = fY / this.mass;
    	this.xxVel += aX * dt;
    	this.yyVel += aY * dt;
    	this.xxPos += this.xxVel * dt;
    	this.yyPos += this.yyVel * dt;
    }

    public void draw() {
    	StdDraw.picture(this.xxPos, this.yyPos, "images/" + this.imgFileName);
    }

}
