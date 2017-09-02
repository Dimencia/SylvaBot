package net.bashtech.geobot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GreenScreen extends JFrame {
	public JLabel timer = new JLabel("5:00");
	private JPanel panel = new JPanel();
	private JLabel topText = new JLabel("Follow Train!");
	private int minutes = 0;
	private int seconds = 0;
	private GreenScreen ref;
	private Image myImage;
	private int imageY = 200;
	private BufferedImage bf;
	private int curColor = 255;
	private int trainSize = 1;
	private JLabel trainSizeLabel = new JLabel();
	
	public GreenScreen() {
		ref = this;
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	    setTitle("SylvaBot");
	    this.setSize(200, 200);
		this.setBackground(new Color(0,255,0));
		timer.setFont(new Font("Helvetica", Font.PLAIN, 40));
		timer.setForeground(Color.BLACK);
		
		myImage = Toolkit.getDefaultToolkit().getImage("sylvafollowtrain.jpg"); 
		
		timer.setHorizontalAlignment(JLabel.CENTER);
		topText.setHorizontalAlignment(JLabel.CENTER);
		
		topText.setFont(new Font("Helvetica", Font.PLAIN, 20));
		
		panel.setLayout(new BorderLayout());
		
		trainSizeLabel.setText("1 New Follow!");
		trainSizeLabel.setFont(new Font("MS Gothic", Font.PLAIN, 15));
		trainSizeLabel.setAlignmentX(CENTER_ALIGNMENT);
		
		JPanel boxPanel = new JPanel();
		boxPanel.setLayout(new BoxLayout(boxPanel,BoxLayout.Y_AXIS));
		topText.setAlignmentX(CENTER_ALIGNMENT);
		timer.setAlignmentX(CENTER_ALIGNMENT);
		
		boxPanel.add(topText);
		boxPanel.add(trainSizeLabel);
		boxPanel.add(timer);
		boxPanel.setBackground(new Color(0,255,0));
		
		panel.add(boxPanel, BorderLayout.NORTH);
		panel.setBackground(new Color(0,255,0));
		this.add(panel,BorderLayout.CENTER);
		
		topText.setVisible(false);
		timer.setVisible(false);
		trainSizeLabel.setVisible(false);
		
		setResizable(false);
		setVisible(true);
	}
	
	public void update(Graphics g){
        paint(g);
	 }
	
	 public void paint(Graphics g){
	
	 bf = new BufferedImage( this.getWidth(),this.getHeight(), BufferedImage.TYPE_INT_RGB);
	
	 try{
	 animation(bf.getGraphics());
	 g.drawImage(bf,0,0,null);
	 }catch(Exception ex){
	
	 }
	}
	
	 public void animation(Graphics g) {
	     super.paint(g);
	     g.drawImage(myImage, 0, imageY, this);
	 }
	
	public void startTimer() {
		if(minutes == 0 && seconds == 0) {
			// Animate the coin? Yup.
			timer.setVisible(false);
			topText.setVisible(false);
			Runnable r = new Runnable() {

				@Override
				public void run() {
					while(imageY > 120)
					{
						imageY--;
						ref.repaint();
					
						try {
							Thread.sleep(25);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					minutes = 5;
					seconds = 0;
					trainSize = 1;
					trainSizeLabel.setText("1 New Follow!");
					timer.setText("5:00");
					timer.setForeground(Color.BLACK);
					topText.setVisible(true);
					timer.setVisible(true);
					trainSizeLabel.setVisible(true);
					
					Runnable r2 = new Runnable() {

						@Override
						public void run() {
							while(minutes >= 0) {
								seconds--;
								if(seconds < 0) {
									minutes--;
									seconds = 59;
								}
								if(minutes < 1) {
									if(curColor == 255) {
										curColor = 150;
										timer.setForeground(new Color(curColor,0,0));
									}
									else {
										curColor = 255;
										timer.setForeground(Color.BLACK);
									}
								}
								if(minutes >= 0){
									timer.setText(String.format("%1$02d:%2$02d", minutes,seconds));
									repaint();
								}
								else {
									timer.setVisible(false);
									topText.setVisible(false);
									trainSizeLabel.setVisible(false);
									imageY = 200;
									repaint();
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							minutes = 0;
							seconds = 0;
							
						}
						
					};
					Thread t2 = new Thread(r2);
					t2.start();
					
				}
				
			};
			Thread t = new Thread(r);
			t.start();
		}
		else {
			trainSize++;
			trainSizeLabel.setText(trainSize + " New Follows!");
			minutes = 5;
			seconds = 0;
		}

	}
	
}
