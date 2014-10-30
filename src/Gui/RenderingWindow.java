/**
 * RenderingWindow.java
 *
 * Created on 11 d�cembre 2005, 16:17
 */

package Gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JOptionPane;

import Raytracer.NoCameraException;
import Raytracer.Raytracer;
import Raytracer.Scene;
import Transformations.ObjectNotSupportedException;

/**
 * JFrame to render the image.
 * 
 * @author Guilhem Duch�
 */
public class RenderingWindow extends javax.swing.JFrame {

	private boolean firstRendering;
	private Scene scene;
	private Raytracer raytracer;
	private Image mImage;
	private String filename;
	private int imageNumber;
	private long begin;
	private long end;

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
	public RenderingWindow(int width, int height, Scene s, boolean sampl,
			String file, Materials.Color color, int brdfType, long beg,
			long en, int nbRec, int operator, double value) {
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
	}

	/**
	 * Method called to repaint.
	 * 
	 * @param g
	 *            the Graphics object where we draw.
	 */
	public void paint(java.awt.Graphics g) {
		super.paint(g);
		if (firstRendering) {
			long time = -System.currentTimeMillis();
			Dimension d = getSize();
			java.awt.Graphics offG = mImage.getGraphics();
			offG.setColor(Color.BLACK);
			offG.fillRect(0, 0, d.width, d.height);
			Graphics2D g2 = (Graphics2D) mImage.getGraphics();
			// Put scene in the correct state to start rendering
			for (long i = 1; i < begin; i++) {
				try {
					raytracer.update(i);
				} catch (ObjectNotSupportedException e) {
					System.out.println(e.getMessage());
				} catch (NoCameraException e) {
					System.out.println(e.getMessage());
				}
			}
			// render all images of the animation
			for (long i = begin; i <= end; i++) {
				raytracer.draw(g2);
				mImage.flush();
				try {
					if (!filename.equals("")) {
						javax.imageio.ImageIO.write((BufferedImage) mImage,
								"PNG", new File(filename + i + ".png"));
					} else {
						javax.imageio.ImageIO.write((BufferedImage) mImage,
								"PNG", new File("default" + i + ".png"));
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				// update postions of objects in the scene.
				try {
					raytracer.update(i);
				} catch (ObjectNotSupportedException e) {
					System.out.println(e.getMessage());
				} catch (NoCameraException e) {
					System.out.println(e.getMessage());
				}
			}
			firstRendering = false;
			time += System.currentTimeMillis();
			JOptionPane.showMessageDialog(null, "Rendering time : "
					+ (end - begin + 1) + " images " + time + "  ms or "
					+ (double) time / 1000 + " sec or " + (double) time
					/ (1000 * 60) + " min!", "Rendered Successfully !",
					JOptionPane.WARNING_MESSAGE);
			// display rendering time.
			g.drawImage(mImage, 0, 0, null);
		} else {
			g.drawImage(mImage, 0, 0, null);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents() {

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);
		pack();
	}
	// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// End of variables declaration//GEN-END:variables

}
