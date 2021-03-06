/*
 * Raytracer.java
 *
 * Created on 11 d�cembre 2005, 20:16
 */

package Raytracer;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import Lights.AmbientLight;
import Lights.PointLight;
import Materials.Color;
import Materials.Material;
import RaytracedObjects.IntersectionResult;
import RaytracedObjects.RaytracedObject;
import Transformations.ObjectNotSupportedException;

/**
 * The Raytracer. Need a CompleteScene object and the dimensions of the image it
 * will have to draw.
 * 
 * @author Guilhem Duch�
 */
public class Raytracer {

	private Scene scene;
	private int width;
	private int height;
	private boolean supersampling;
	private int brdf;
	private int maxDepth;
	private boolean inside;
	private double[][] redImage;
	private double[][] greenImage;
	private double[][] blueImage;
	private int selectedOperator;
	private double lmax;
	private Color backgroundColor;

	public final static int PHONG = 0;
	public final static int PHONGBLINN = 1;
	public final static int NONE = 0;
	public final static int WARD = 1;
	public final static int REINHARD = 2;
	public final static double EPSILON = 0.00001;
	private final static double LDMAX = 100;
	private final static double AGRAYZONE = 0.18;

	/**
	 * Creates a new instance of Raytracer.
	 * 
	 * @param ss
	 *            the CompleteScene which will be drawn.
	 * @param w
	 *            the width of the image to be rendered.
	 * @param h
	 *            the height of the image to be rendered.
	 * @param sampl
	 *            indicates if supersampling is activated.
	 * @param color
	 *            the background color.
	 * @param brdfType
	 *            illumination model used. 0 if phong, 1 if phong-blinn.
	 * @param maxDep
	 *            the maximum number of recursivity.
	 * @param operator
	 *            the operator to apply at the end.
	 * @param value
	 *            the value for LMax for the operator.
	 */
	public Raytracer(Scene ss, int w, int h, boolean sampl, Color color,
			int brdfType, int maxDep, int operator, double value) {
		scene = ss;
		width = w;
		height = h;
		supersampling = sampl;
		brdf = brdfType;
		maxDepth = maxDep;
		inside = false;
		redImage = new double[width][height];
		greenImage = new double[width][height];
		blueImage = new double[width][height];
		selectedOperator = operator;
		lmax = value;
		backgroundColor = color;
	}

	/**
	 * Update the CompleteScene according to the current time.
	 * 
	 * @param time
	 *            the time in the scene.
	 */
	public void update(long time) throws ObjectNotSupportedException,
			NoCameraException {
		scene.update(time);
	}

	/**
	 * draw the image on the given Graphics2D object.
	 * 
	 * @param g2
	 *            the Graphics2D object where we draw.
	 */
	public void draw(Graphics2D g2) {
		// launching rays.
		Camera camera = scene.getCamera();
		// prepare to throw rays
		Point3d eyePoint = camera.getEyePoint();// eyepoint in world
												// coordinates.
		double distanceCameraPlane = camera.getDistanceEyeVpn();// get distance
																// eye view
																// plane
		double heightPlane = camera.getHeightVpn();// get size of view plane
		double widthPlane = width * heightPlane / height;// compute size of the
															// vpn
		double dxCamCoord = widthPlane / width;// dx movement of the point which
												// we go through to launch the
												// ray in Camera Coordinates
		double dyCamCoord = heightPlane / height;// dy movement of the point
													// which we go through to
													// launch the ray in CC
		Vector4d departurePointCamCoord = new Vector4d(-widthPlane / 2,
				heightPlane / 2, distanceCameraPlane, 1);// departure point in
															// CC
		Vector4d secondPointCamCoord = new Vector4d(-widthPlane / 2
				+ dxCamCoord, heightPlane / 2, distanceCameraPlane, 1);// second
																		// point
																		// in CC
		Vector4d secondLineCamCoord = new Vector4d(-widthPlane / 2, heightPlane
				/ 2 - dyCamCoord, distanceCameraPlane, 1);// first point of
															// second line in CC
		Matrix4d transform = camera.getCameraToWorldTransform();
		// set departure point and dx and dy in world coordinates.
		transform.transform(departurePointCamCoord);// transform the departure
													// point in World
													// coordinates
		transform.transform(secondPointCamCoord);// transform second point in wc
													// to determine later dx in
													// WC
		transform.transform(secondLineCamCoord);// transform first point of
												// second line in wc to
												// determine later dy in WC
		Vector3d departurePoint = new Vector3d(departurePointCamCoord.x,
				departurePointCamCoord.y, departurePointCamCoord.z);// will be
																	// used to
																	// find
																	// easily
																	// the
																	// beginning
																	// of a
																	// line.
		Vector3d currentPoint = (Vector3d) departurePoint.clone();// point used
																	// to throw
																	// the rays
		// dx movement of the point which we go through to launch the ray in WC
		Vector3d dxVect = new Vector3d(
				secondPointCamCoord.x - departurePoint.x, secondPointCamCoord.y
						- departurePoint.y, secondPointCamCoord.z
						- departurePoint.z);
		// dy movement of the point which we go through to launch the ray in WC
		Vector3d dyVect = new Vector3d(secondLineCamCoord.x - departurePoint.x,
				secondLineCamCoord.y - departurePoint.y, secondLineCamCoord.z
						- departurePoint.z);

		// dx and dy for supersampling
		Vector3d ddxVect = new Vector3d(dxVect.x / 4.0, dxVect.y / 4.0,
				dxVect.z / 4.0);
		Vector3d ddyVect = new Vector3d(dyVect.x / 4.0, dyVect.y / 4.0,
				dyVect.z / 4.0);
		// calculate image
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color col = new Color(0, 0, 0);
				if (supersampling) {
					// get direction and normalize it
					Vector3d direction = new Vector3d(currentPoint.x
							- ddxVect.x - eyePoint.x, currentPoint.y
							- ddxVect.y - eyePoint.y, currentPoint.z
							- ddxVect.z - eyePoint.z);
					direction.normalize();
					Color col1 = illumination(eyePoint, direction, 1, null);
					direction = new Vector3d(currentPoint.x + ddxVect.x
							- eyePoint.x, currentPoint.y + ddxVect.y
							- eyePoint.y, currentPoint.z + ddxVect.z
							- eyePoint.z);
					direction.normalize();
					Color col2 = illumination(eyePoint, direction, 1, null);
					direction = new Vector3d(currentPoint.x + ddyVect.x
							- eyePoint.x, currentPoint.y + ddyVect.y
							- eyePoint.y, currentPoint.z + ddyVect.z
							- eyePoint.z);
					direction.normalize();
					Color col3 = illumination(eyePoint, direction, 1, null);
					direction = new Vector3d(currentPoint.x - ddyVect.x
							- eyePoint.x, currentPoint.y - ddyVect.y
							- eyePoint.y, currentPoint.z - ddyVect.z
							- eyePoint.z);
					direction.normalize();
					Color col4 = illumination(eyePoint, direction, 1, null);
					redImage[x][y] = (col1.getRed() + col2.getRed()
							+ col3.getRed() + col4.getRed()) / 4;
					greenImage[x][y] = (col1.getGreen() + col2.getGreen()
							+ col3.getGreen() + col4.getGreen()) / 4;
					blueImage[x][y] = (col1.getBlue() + col2.getBlue()
							+ col3.getBlue() + col4.getBlue()) / 4;
				} else {
					// get direction and normalize it
					Vector3d direction = new Vector3d(currentPoint.x
							- eyePoint.x, currentPoint.y - eyePoint.y,
							currentPoint.z - eyePoint.z);
					direction.normalize();
					// launch the Ray in the scene
					col = illumination(eyePoint, direction, 1, null);
					redImage[x][y] = col.getRed();
					greenImage[x][y] = col.getGreen();
					blueImage[x][y] = col.getBlue();
				}
				// go to the next in worldCoordinate
				currentPoint.add(dxVect);
			}
			// go to the next line in world coordinate
			departurePoint.add(dyVect);
			currentPoint = (Vector3d) departurePoint.clone();
		}
		// apply operators if needed.
		if (selectedOperator == WARD || selectedOperator == REINHARD) {
			double lwAvg = 0;
			// scale by Lmax value
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					redImage[x][y] *= lmax;
					greenImage[x][y] *= lmax;
					blueImage[x][y] *= lmax;
					lwAvg += Math.log(EPSILON + 0.27 * redImage[x][y] + 0.67
							* greenImage[x][y] + 0.06 * blueImage[x][y]);
				}
			}

			// Lw average
			lwAvg = Math.exp(lwAvg / (width * height));

			// apply good operator
			if (selectedOperator == WARD) {// apply Ward operator
				double lwa = lwAvg;
				double sf = Math.pow((1.219 + Math.pow(LDMAX / 2, 0.4))
						/ (1.219 + Math.pow(lwa, 0.4)), 2.5)
						/ LDMAX;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						redImage[x][y] *= sf;
						greenImage[x][y] *= sf;
						blueImage[x][y] *= sf;
					}
				}
				// go back to RGB values
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						redImage[x][y] /= lmax;
						greenImage[x][y] /= lmax;
						blueImage[x][y] /= lmax;
					}
				}
				// end ward operator
			} else if (selectedOperator == REINHARD) {// apply Reinhard operator
				double coeff = AGRAYZONE / lwAvg;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						redImage[x][y] *= coeff;
						greenImage[x][y] *= coeff;
						blueImage[x][y] *= coeff;
						redImage[x][y] = (redImage[x][y] / (1 + redImage[x][y]))
								* LDMAX;
						greenImage[x][y] = (greenImage[x][y] / (1 + greenImage[x][y]))
								* LDMAX;
						blueImage[x][y] = (blueImage[x][y] / (1 + blueImage[x][y]))
								* LDMAX;
					}
				}
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						redImage[x][y] /= LDMAX;
						greenImage[x][y] /= LDMAX;
						blueImage[x][y] /= LDMAX;
					}
				}
			}// end reinhard operator

		}// end operators

		// draw final image
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// System.out.println(redImage[x][y]+" "+greenImage[x][y]+" "+blueImage[x][y]);
				java.awt.Color col = new java.awt.Color((float) redImage[x][y],
						(float) greenImage[x][y], (float) blueImage[x][y]);
				g2.setColor(col);
				g2.drawRect(x, y, 1, 1);
			}
		}
	}

	/**
	 * Illumination calculation. Algorithm of the recursice RayTracing.
	 * 
	 * @param eyePoint
	 *            the origin of the ray.
	 * @param direction
	 *            the direction of the ray.
	 * @param depth
	 *            the depth of recursive raytracing.
	 * @param obj
	 *            the last RaytracedObject.
	 * @return The color of the result.
	 */
	public Color illumination(Point3d eyePoint, Vector3d direction, int depth,
			RaytracedObject obj) {
		boolean save = inside;
		IntersectionResult resul = scene.getResul(eyePoint, direction, obj);
		// Color global = new java.awt.Color(0,0.7,0.78);
		Color global = (Color) backgroundColor.clone();
		if (resul != null) {// we touched something calculate color.
			// calculate local illumination
			global = globalIllumination(eyePoint, direction, resul);
			// System.out.println(global);
			if (depth < maxDepth) {
				Material mat = resul.getMaterial();
				Color kr = mat.getKr();
				Color kt = mat.getKt();
				// reflexion
				if (kr.red > 0 || kr.green > 0 || kr.blue > 0) {// spawn
																// reflection
																// ray
					// calculate reflexion vector
					Vector3d r = (Vector3d) direction.clone();
					// r.negate();
					Vector3d toSub = (Vector3d) (resul.getNormal()).clone();
					double plop = 2
							* r.dot(resul.getNormal())
							/ (resul.getNormal().length() * resul.getNormal()
									.length());
					toSub.scale(plop);
					r.sub(toSub);
					r.normalize();
					Color reflect = illumination(resul.getIntersectionPoint(),
							r, depth + 1, resul.getObjectTouched());
					double red = adjust(global.getRed() + kr.red
							* reflect.getRed());
					double green = adjust(global.getGreen() + kr.green
							* reflect.getGreen());
					double blue = adjust(global.getBlue() + kr.blue
							* reflect.getBlue());
					global = new Color(red, green, blue);
				}

				// transmission
				if (kt.red > 0 || kt.green > 0 || kt.blue > 0) {// spawn
																// transmission
																// ray
					double ni = 1;
					double nt = 1;
					Vector3d normal = resul.getNormal();
					if (!inside) {
						ni = 1.0;
						nt = mat.getNi();
						if (resul.getObjectTouched().hasInterior())
							inside = true;
					} else {
						ni = mat.getNi();
						nt = 1.0;
						inside = false;
					}
					double div = ni / nt;
					Vector3d direc = (Vector3d) direction.clone();
					direc.negate();
					double dotProd = direction.dot(normal);
					double sndTerm = 1 - div * div * (1 - dotProd * dotProd);
					Color trans;
					double red = global.getRed();
					double green = global.getGreen();
					double blue = global.getBlue();
					if (sndTerm >= 0) {
						Vector3d t = (Vector3d) direction.clone();
						Vector3d nn = (Vector3d) normal.clone();
						nn.scale(dotProd);
						t.sub(nn);
						t.scale(div);
						Vector3d sndVec = (Vector3d) normal.clone();
						sndVec.scale(Math.sqrt(sndTerm));
						t.sub(sndVec);
						t.normalize();
						trans = illumination(resul.getIntersectionPoint(), t,
								depth + 1, resul.getObjectTouched());
						// todo check ...
						// if (inside){
						red = adjust(red + kt.red * trans.getRed());
						green = adjust(green + kt.green * trans.getGreen());
						blue = adjust(blue + kt.blue * trans.getBlue());
						/*
						 * }else{ red = adjust(red+trans.getRed()/255.0); green
						 * = adjust(green+trans.getGreen()/255.0); blue =
						 * adjust(blue+trans.getBlue()/255.0); }
						 */
					} else {// in case of internal transion
							// calculate reflexion vector
						Vector3d r = (Vector3d) direction.clone();
						Vector3d toSub = (Vector3d) (resul.getNormal()).clone();
						double plop = 2
								* r.dot(resul.getNormal())
								/ (resul.getNormal().length() * resul
										.getNormal().length());
						toSub.scale(plop);
						r.sub(toSub);
						r.normalize();
						trans = illumination(resul.getIntersectionPoint(), r,
								depth + 1, resul.getObjectTouched());
						red = adjust(red + kt.red * trans.getRed());
						green = adjust(green + kt.green * trans.getGreen());
						blue = adjust(blue + kt.blue * trans.getBlue());
					}
					global = new Color(red, green, blue);
				}
			}
			inside = save;
			return global;
		} else {
			inside = save;
			return global;
		}
	}

	/**
	 * Ask the selected illumation model the color of the GlobalIllumination for
	 * the given result.
	 * 
	 * @param eyePoint
	 *            the origin of the ray.
	 * @param direction
	 *            the direction of the ray.
	 * @return The color of the result.
	 */
	public Color globalIllumination(Point3d eyePoint, Vector3d direction,
			IntersectionResult resul) {
		// calculate local illumination
		if (brdf == PHONG) {
			return phong(eyePoint, direction, resul);
		} else if (brdf == PHONGBLINN) {
			return phongBlinn(eyePoint, direction, resul);
		} else {
			return (Color) backgroundColor.clone();
		}
	}

	/**
	 * Phong illumination model.
	 * 
	 * @param eyePoint
	 *            the origin of the ray.
	 * @param direction
	 *            the direction of the ray.
	 * @param resul
	 *            the InterSectionResult of the ray with the scene.
	 * @return The color of the result.
	 */
	public Color phong(Point3d eyePoint, Vector3d direction,
			IntersectionResult resul) {
		// calculate the color
		Material material = resul.getMaterial();
		ArrayList<PointLight> lights = scene.getLights();
		AmbientLight ambientLight = scene.getAmbientLight();
		double red = 0;
		double green = 0;
		double blue = 0;

		// add ambient light
		if (ambientLight != null) {
			red += material.getKa().red * ambientLight.getRed()
					* material.getAmbientColor().red;
			green += material.getKa().green * ambientLight.getGreen()
					* material.getAmbientColor().green;
			blue += material.getKa().blue * ambientLight.getBlue()
					* material.getAmbientColor().blue;
		}

		double diffRed = 0;
		double diffGreen = 0;
		double diffBlue = 0;

		double specRed = 0;
		double specGreen = 0;
		double specBlue = 0;

		// add diffuse light and specular light if the light is seen
		Iterator<PointLight> it = lights.iterator();
		while (it.hasNext()) {// for each light
			PointLight currentLight = it.next();
			Point3d inter = resul.getIntersectionPoint();
			Vector3d n = resul.getNormal();
			Vector3d s = new Vector3d(currentLight.getPointLight().x - inter.x,
					currentLight.getPointLight().y - inter.y,
					currentLight.getPointLight().z - inter.z);
			double distanceIntersectionLight = s.length();
			s.normalize();
			IntersectionResult res2 = scene.getResul(inter, s,
					resul.getObjectTouched());// check the getTouched is it
												// necessary ? (special case
												// bottom of the sphere...)
			double mult = s.dot(n);
			if ((res2 == null || distanceIntersectionLight < res2.getDistance())
					&& mult > 0) {// to change not collide with the object from
									// wich we send the ray (null for normal
									// case)
				// there is no object between the intersection point and the
				// light
				// or the object touched is further than the light from the
				// intersection point and so doesn't put the intersection point
				// into shadow.
				// the object is exposed to the light (s.n>0)

				// diffuse radiance for this light
				double liDiffRed = currentLight.getRed(inter)
						* material.getDiffuseColor().red;
				double liDiffGreen = currentLight.getGreen(inter)
						* material.getDiffuseColor().green;
				double liDiffBlue = currentLight.getBlue(inter)
						* material.getDiffuseColor().blue;

				// diffuse light value
				diffRed += liDiffRed * mult;
				diffGreen += liDiffGreen * mult;
				diffBlue += liDiffBlue * mult;
				if (diffBlue < 0 || diffGreen < 0 || diffRed < 0) {// debugging
					System.out.println("diffuse light negative : normal " + n
							+ " direction " + direction + " red " + diffRed
							+ " green " + diffGreen + " blue " + diffRed
							+ " object : " + resul.getObjectTouched());
					boolean a = false;
					if (a) {
						diffRed = Math.abs(diffRed);
						diffGreen = Math.abs(diffGreen);
						diffBlue = Math.abs(diffBlue);
					}
				}

				// specular radiance for this light
				double liSpecRed = currentLight.getRed(inter)
						* material.getSpecColor().red;
				double liSpecGreen = currentLight.getGreen(inter)
						* material.getSpecColor().green;
				double liSpecBlue = currentLight.getBlue(inter)
						* material.getSpecColor().blue;

				// specular light value
				Vector3d v = new Vector3d(-direction.x, -direction.y,
						-direction.z);
				// calculate reflexion vector
				Vector3d r = (Vector3d) s.clone();
				// r.negate();
				Vector3d toSub = (Vector3d) n.clone();
				double plop = 2 * r.dot(n) / (n.length() * n.length());
				toSub.scale(plop);
				r.sub(toSub);
				r.normalize();
				// add specular light
				specRed += liSpecRed * Math.pow(r.dot(v), material.getKe().red);
				specGreen += liSpecGreen
						* Math.pow(r.dot(v), material.getKe().green);
				specBlue += liSpecBlue
						* Math.pow(r.dot(v), material.getKe().blue);
				if (specRed < 0 || specGreen < 0 || specBlue < 0) {// debugging
					// System.out.println("specular light negative : normal "+n+" direction "+direction+" red "+specRed+" green "+specGreen+" blue "+specBlue+" object "+resul.getObjectTouched());
				}
			} else {// there is an object between light and intersection point
					// the point is into shadow
					// nothing to do
					// the point is lit only by ambient light

			}
		}
		// add diffuse light and specular light (multiplied by the right
		// coefficient) to the result color
		red = red + material.getKd().red * diffRed + material.getKs().red
				* specRed;
		green = green + material.getKd().green * diffGreen
				+ material.getKs().green * specGreen;
		blue = blue + material.getKd().blue * diffBlue + material.getKs().blue
				* specBlue;
		return new Color(adjust(red), adjust(green), adjust(blue));
	}

	/**
	 * Phong-Blinn illumination model.
	 * 
	 * @param eyePoint
	 *            the origin of the ray.
	 * @param direction
	 *            the direction of the ray.
	 * @return The color of the result.
	 */
	public Color phongBlinn(Point3d eyePoint, Vector3d direction,
			IntersectionResult resul) {
		// calculate the color
		Material material = resul.getMaterial();
		ArrayList<PointLight> lights = scene.getLights();
		AmbientLight ambientLight = scene.getAmbientLight();
		double red = 0;
		double green = 0;
		double blue = 0;

		// add ambient light
		if (ambientLight != null) {
			red += material.getKa().red * ambientLight.getRed()
					* material.getAmbientColor().red;
			green += material.getKa().green * ambientLight.getGreen()
					* material.getAmbientColor().green;
			blue += material.getKa().blue * ambientLight.getBlue()
					* material.getAmbientColor().blue;
		}

		double diffRed = 0;
		double diffGreen = 0;
		double diffBlue = 0;

		double specRed = 0;
		double specGreen = 0;
		double specBlue = 0;

		// add diffuse light and specular light if the light is seen
		Iterator<PointLight> it = lights.iterator();
		while (it.hasNext()) {// for each light
			PointLight currentLight = it.next();
			Point3d inter = resul.getIntersectionPoint();
			Vector3d n = resul.getNormal();
			Vector3d s = new Vector3d(currentLight.getPointLight().x - inter.x,
					currentLight.getPointLight().y - inter.y,
					currentLight.getPointLight().z - inter.z);
			double distanceIntersectionLight = s.length();
			s.normalize();
			IntersectionResult res2 = scene.getResul(inter, s,
					resul.getObjectTouched());// check the getTouched is it
												// necessary ? (special case
												// bottom of the sphere...)
			double mult = s.dot(n);
			if ((res2 == null || distanceIntersectionLight < res2.getDistance())
					&& mult > 0) {// to change not collide with the object from
									// wich we send the ray (null for normal
									// case)
				// there is no object between the intersection point and the
				// light
				// or the object touched is further than the light from the
				// intersection point and so doesn't put the intersection point
				// into shadow.

				// diffuse radiance for this light
				double liDiffRed = currentLight.getRed(inter)
						* material.getDiffuseColor().red;
				double liDiffGreen = currentLight.getGreen(inter)
						* material.getDiffuseColor().green;
				double liDiffBlue = currentLight.getBlue(inter)
						* material.getDiffuseColor().blue;

				// diffuse light value
				diffRed += liDiffRed * mult;
				diffGreen += liDiffGreen * mult;
				diffBlue += liDiffBlue * mult;// return
												// material.getAmbientColor();
				if (diffBlue < 0 || diffGreen < 0 || diffRed < 0) {// debugging
					System.out.println("diffuse light negative : normal " + n
							+ " direction " + direction + " red " + diffRed
							+ " green " + diffGreen + " blue " + diffRed
							+ " object : " + resul.getObjectTouched());
				}

				// specular radiance for this light
				double liSpecRed = currentLight.getRed(inter)
						* material.getSpecColor().red;
				double liSpecGreen = currentLight.getGreen(inter)
						* material.getSpecColor().green;
				double liSpecBlue = currentLight.getBlue(inter)
						* material.getSpecColor().blue;

				// specular light value
				Vector3d v = new Vector3d(-direction.x, -direction.y,
						-direction.z);
				// calculate reflexion vector
				Vector3d halfwayvector = new Vector3d((v.x + s.x) / 2,
						(v.y + s.y) / 2, (v.z + s.z) / 2);
				// add specular light
				specRed += liSpecRed
						* Math.pow(halfwayvector.dot(n), material.getKe().red);
				specGreen += liSpecGreen
						* Math.pow(halfwayvector.dot(n), material.getKe().green);
				specBlue += liSpecBlue
						* Math.pow(halfwayvector.dot(n), material.getKe().blue);
				if (specRed < 0 || specGreen < 0 || specBlue < 0) {// debugging
					System.out.println("specular light negative : normal " + n
							+ " direction " + direction + " red " + specRed
							+ " green " + specGreen + " blue " + specBlue
							+ " object " + resul.getObjectTouched());
				}
			} else {// there is an object between light and intersection point
					// the point is into shadow
					// nothing to do
					// the point is lit only by ambient light
			}
		}
		// add diffuse light and specular light (multiplied by the right
		// coefficient) to the result color
		red = red + material.getKd().red * diffRed + material.getKs().red
				* specRed;
		green = green + material.getKd().green * diffGreen
				+ material.getKs().green * specGreen;
		blue = blue + material.getKd().blue * diffBlue + material.getKs().blue
				* specBlue;
		return new Color(adjust(red), adjust(green), adjust(blue));
	}

	/**
	 * Adjust a number if too big or too small.
	 * 
	 * @param a
	 *            the number to adjust.
	 * @return if (a < 0) return 0 if (a > 1) return 1 else return a;
	 */
	private double adjust(double a) {

		if (a > 1) {
			return 1;
		} else if (a < 0) {
			return 0;
		} else
			return a;
	}
}
/*
 * // tricky stuff use incoming lights to calculate reflections ray; not
 * realistic but fun //code to be used replacing the reflexion calculation.
 * ArrayList<PointLight> lights = scene.getLights(); Iterator<PointLight> it =
 * lights.iterator(); while(it.hasNext()){//for each light //compute the normal
 * PointLight currentLight = it.next(); Point3d inter =
 * resul.getIntersectionPoint(); Vector3d n = resul.getNormal();
 * 
 * Vector3d r = new
 * Vector3d(currentLight.getX()-inter.x,currentLight.getY()-inter
 * .y,currentLight.getZ()-inter.z); r.negate(); Vector3d toSub =
 * (Vector3d)n.clone(); double plop = 2*r.dot(n)/(n.length()*n.length());
 * toSub.scale(plop); r.sub(toSub); r.normalize(); java.awt.Color reflect =
 * illumination(resul.getIntersectionPoint(),r,depth+1,
 * resul.getObjectTouched()); float red =
 * adjust(global.getRed()/255.0+kr.red*reflect.getRed()/255.0); float green =
 * adjust(global.getGreen()/255.0+kr.green*reflect.getGreen()/255.0); float blue
 * = adjust(global.getBlue()/255.0+kr.blue*reflect.getBlue()/255.0); global =
 * new java.awt.Color(red,green,blue); }
 */
