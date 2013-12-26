package io.pyd.synchro.mocks;

import java.util.Locale;
import java.util.ResourceBundle;

import io.pyd.synchro.CoreManager;

/**
 * This is a mock for CoreManager - will be used for JUnit testing purposes
 * has own constructor and getInstance() method for providing instance to all other classes needed
 * @author WojT
 *
 */
public class CoreManagerMock extends CoreManager {

	public CoreManagerMock(Locale locale, boolean daemon) {
		super(locale, daemon);
	}
	public static void instanciate(Locale locale, boolean daemon){
		CoreManagerMock.instance = new CoreManagerMock(locale, daemon);
	}
	public static CoreManagerMock getInstance(){
		return (CoreManagerMock) CoreManagerMock.instance;
	}
		
}
