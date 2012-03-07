package info.ajaxplorer.synchro.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class ConfigEditor {	
	
	public void initShell(final Shell shell){
		
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				shell.setVisible(false);
				event.doit = false;
			}
		});		
		
		shell.addListener(SWT.MIN, new Listener() {
			public void handleEvent(Event event) {
				shell.setVisible(false);
				event.doit = false;
			}
	    });		
		
        shell.setText("AjaXplorer Synchronizer");
        shell.setSize(450, 400);
        shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));
        
        Point p = shell.getSize();

        shell.setBounds(150, 150, p.x, p.y);        
        new JobEditor(shell, 0);        
        shell.pack();
        
	}	
	
}
