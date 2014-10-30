/**
 * RenderingWindow.java
 *
 * Created on 11 décembre 2005, 16:17
 */

package Gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.vecmath.Vector3d;

import Raytracer.Camera;
import Raytracer.Raytracer;
import Raytracer.Scene;

/**
 * JFrame to render the image.
 * 
 * @author Guilhem Duché
 */
public class RenderingWindowRealTime extends javax.swing.JFrame implements
		KeyListener {

	private boolean firstRendering;
	private Scene scene;
	private Raytracer raytracer;
	private Image mImage;
	private String filename;
	private int imageNumber;
	private long begin;
	private long end;

	/* movements vector */
	private static Vector3d MOVE_FORWARD = new Vector3d(1, 0, 0);
	private static Vector3d MOVE_BACKWARD = new Vector3d(-1, 0, 0);
	private static Vector3d STRAFE_LEFT = new Vector3d(0, 1, 0);
	private static Vector3d STRAFE_RIGHT = new Vector3d(0, -1, 0);

	/**
	 * Creates new form RenderingWindow.
	 * 
	 * @param width
	 *            X size of the image.
	 * @param height
	 *            Y size of the rendered image.
	 * @param s
	 *            the CompleteScene to draw.
	 * @param sampl
	 *            indicates if supersampling is activated.
	 * @param file
	 *            name of the file to write.
	 * @param color
	 *            the background color.
	 * @param brdfType
	 *            illumination model used. 0 if phong, 1 if phong-blinn.
	 * @param beg
	 *            the date of beginning.
	 * @param en
	 *            the date of end.
	 * @param nbRec
	 *            number of recursivity for the raytracer.
	 * @param operator
	 *            the operator to apply at the end.
	 * @param value
	 *            the value for LMax for the operator.
	 */
	public RenderingWindowRealTime(int width, int height, Scene s,
			boolean sampl, String file, Materials.Color color, int brdfType,
			long beg, long en, int nbRec, int operator, double value) {
		super();
		initComponents();
		setSize(new Dimension(width, height));
		scene = s;
		raytracer = new Raytracer(scene, width, height, sampl, color, brdfType,
				nbRec, operator, value);
		firstRendering = true;
		mImage = createImage(width, height);
		filename = file;
		begin = beg;
		end = en;
		addKeyListener(this);
		System.out.println("construct");
	}

	/**
	 * Method called to repaint.
	 * 
	 * @param g
	 *            the Graphics object where we draw.
	 */
	public void paint(java.awt.Graphics g) {
		super.paint(g);
		Dimension d = getSize();
		java.awt.Graphics offG = mImage.getGraphics();
		offG.setColor(Color.BLACK);
		offG.fillRect(0, 0, d.width, d.height);
		Graphics2D g2 = (Graphics2D) mImage.getGraphics();
		System.out.println("test");
		raytracer.draw(g2);
		mImage.flush();
		g.drawImage(mImage, 0, 0, null);
	}

	private void initComponents() {

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);
		pack();
	}

	public void keyPressed(KeyEvent e) {
		System.out.println("plop");
		Camera cam = scene.getCamera();
		if (e.getKeyCode() == KeyEvent.VK_UP) {
			cam.moveEyepoint(RenderingWindowRealTime.MOVE_FORWARD);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			cam.moveEyepoint(RenderingWindowRealTime.MOVE_BACKWARD);
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			cam.moveEyepoint(RenderingWindowRealTime.STRAFE_LEFT);
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			cam.moveEyepoint(RenderingWindowRealTime.STRAFE_RIGHT);
		}
		repaint();
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

}
