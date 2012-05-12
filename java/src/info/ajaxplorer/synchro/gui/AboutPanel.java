package info.ajaxplorer.synchro.gui;

import org.eclipse.nebula.animation.AnimationRunner;
import org.eclipse.nebula.animation.effects.AlphaEffect;
import org.eclipse.nebula.animation.movement.ExpoOut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AboutPanel extends Composite {

	private SysTray tray; 
	private boolean ready = false;
	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}
	
	public AboutPanel(final Shell shell, SysTray tray) {
		super(shell, SWT.WRAP);
		this.tray = tray;
		buildInterface();	
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {
				if(!ready) return;
				new AnimationRunner().runEffect(
						new AlphaEffect(
								getShell(), 
								255 /*initial value*/, 
								0 /*final value*/, 
								1500 /*duration*/, 
								new ExpoOut() /*movement*/, 
								new Runnable() {
									@Override
									public void run() {
										AboutPanel.this.tray.closeAboutPane(shell);						
									}
								},
								null /*run on cancel*/
								));				
								
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		this.setLayout(new FillLayout(SWT.HORIZONTAL|SWT.VERTICAL));
		shell.pack();
		
		shell.setAlpha(0);
		shell.setVisible(true);
		shell.forceActive();
		new AnimationRunner().runEffect(
				new AlphaEffect(
						getShell(), 
						0 /*initial value*/, 
						255 /*final value*/, 
						1500 /*duration*/, 
						new ExpoOut() /*movement*/, 
						new Runnable() {
							public void run() {
								ready = true;
							}
						},
						null /*run on cancel*/
						));				
	}
	
	private void buildInterface(){
		SWTResourceManager.registerResourceUser(this);
		Rectangle screen = getShell().getDisplay().getPrimaryMonitor().getClientArea();
		int left = (screen.width -300) / 2; 
		int top = (screen.height -250) / 2; 
		getShell().setBounds(left, top, 300, 250);
		this.setSize(300, 250);
		this.setBackgroundImage(new Image(getDisplay(), new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/AboutPane.png"))));
		
		Label label = new Label(this, SWT.NULL);
		label.setBounds(1, 219, 298, 30);
		label.setAlignment(SWT.CENTER|SWT.BOTTOM);
		label.setBackground(SWTResourceManager.getColor(255, 255, 255));
		label.setText("AjaXplorer Synchronization Tool - v1.0");
		
	}

}
