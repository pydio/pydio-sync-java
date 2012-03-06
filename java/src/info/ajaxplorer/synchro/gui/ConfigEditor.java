package info.ajaxplorer.synchro.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

public class ConfigEditor {	
	
	public void initShell(final Shell shell){
		
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				shell.setVisible(false);
				event.doit = false;
			}
	    });		
		
        shell.setText("AjaXplorer Synchronizer");
        shell.setSize(450, 400);

        //Rectangle bds = shell.getDisplay().getBounds();
        Point p = shell.getSize();

        //int nLeft = (bds.width - p.x) / 2;
        //int nTop = (bds.height - p.y) / 2;

        shell.setBounds(150, 150, p.x, p.y);
        
        JobEditor jobEdit = new JobEditor(shell, 0);
        
        shell.pack();
        
	}	
	
}
