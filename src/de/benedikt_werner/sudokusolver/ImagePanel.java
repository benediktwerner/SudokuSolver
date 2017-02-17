package de.benedikt_werner.sudokusolver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

//Extends JPanel to have an Image as background
public class ImagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private Image img;
	private BufferedImage[][] images;
	private int type = 0;
	private Point[] corners;
	
	public ImagePanel(BufferedImage img) {
		super();
		
		this.img = img;
		
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));

		setupMouseListener();
	}
	
	public ImagePanel(BufferedImage[][] images) {
		super();
		
		type = 1;
		this.images = images;
		
		setPreferredSize(new Dimension(1000, 1000));
		
		setupMouseListener();
	}
	
	public ImagePanel(BufferedImage img, Point[] corners) {
		super();
		
		type = 2;
		this.img = img;
		this.corners = corners;
		
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		
		setupMouseListener();
	}
	
	//Paint the background and all other components
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (type == 0)
			g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
		else if (type == 1) {
			int width = 0;
			int height = 0;
			for (int x = 0; x < images.length; x++) {
				for (int y = 0; y < images[0].length; y++) {
					if (images[x][y] == null) continue;

					if (images[x][y].getWidth() > width) width = images[x][y].getWidth();
					if (images[x][y].getHeight() > height) height = images[x][y].getHeight();
					g.drawImage(images[x][y], x * (width + 5), y * (height + 5), images[x][y].getWidth(), images[x][y].getHeight(), null);
				}
			}
		}
		else {
			g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
			
			g.setColor(Color.RED);
			for (Point p : corners) {
				g.fillRect(p.x, p.y, 1, 1);
			}
		}
	    
	    Point mouse = getMousePosition();
	    if (mouse != null)
	    	g.drawString(mouse.x + " | " + mouse.y, 0, getHeight());
	}
	
	public void setupMouseListener() {
		addMouseMotionListener(new MouseMotionListener() {

	        @Override
	        public void mouseMoved(MouseEvent e) {
	        repaint();
	        }

	        @Override
	        public void mouseDragged(MouseEvent e) {
	        }
	    });
	}
}
