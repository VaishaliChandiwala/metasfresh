/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved. *
 * This program is free software; you can redistribute it and/or modify it *
 * under the terms version 2 of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. *
 * See the GNU General Public License for more details. *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 * For the text or an alternative of this public license, you may reach us *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA *
 * or via info@compiere.org or http://www.compiere.org/license.html *
 *****************************************************************************/
package org.compiere;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.Adempiere.RunMode;
import org.compiere.model.ModelValidationEngine;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Util;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import de.metas.boot.Metasfresh;
import de.metas.logging.LogManager;
import lombok.NonNull;

/**
 * Adempiere Control Class
 *
 * @author Jorg Janke
 * @version $Id: Adempiere.java,v 1.8 2006/08/11 02:58:14 jjanke Exp $
 *
 */
public class Adempiere
{
	public static final Adempiere get()
	{
		return instance;
	}

//	public static final String BEANNAME = "adempiere";

	/**
	 * The "raw" unsubstituted version string from <code>/de.metas.endcustomer..base/src/main/resources/org/adempiere/version.properties</code>
	 */
	public static final String CLIENT_VERSION_UNPROCESSED = "${env.MF_VERSION}";

	/**
	 * The <code>env.MF_VERSION</code> value set by maven if run locally (as opposed to the CI system).<br>
	 * Please keep it in sync with the <code>build-version-env-missing</code> profile of the <a href="https://github.com/metasfresh/metasfresh-parent">metasfresh-parent</a> <code>pom.xml</code>.
	 */
	public static final String CLIENT_VERSION_LOCAL_BUILD = "LOCAL-NO-RELEASE";

	private static final transient Adempiere instance = new Adempiere();

	/**
	 * Client language to use. If set as a system property, then. no language combo box is show on startup and the given language is used instead. Task 06664.
	 */
	public final static String PROPERTY_DefaultClientLanguage = "org.adempiere.client.lang";

	/** Main Version String */
	private static String _mainVersion = "";
	/** Detail Version as date Used for Client/Server */
	private static String _dateVersion = "";
	/** Database Version as date Compared with AD_System */
	private static String _databaseVersion = "";
	private static String _implementationVersion = null;
	private static String _implementationVendor = null;
	/** Product Name */
	private static String _productName = "ERP";
	private static String _subTitle = "";
	private static String _brandCopyright = "";
	private static String _copyright = "";
	/** URL of Product */
	private static String _productUrl = "";
	private static String _onlineHelpUrl = "";
	/** Support Email */
	private static String _supportEmail = "";

	/** Product icon name (small) */
	private static String _productIconSmallName = null;
	private static Image _productIconSmall;

	/** Product logo name (small) */
	private static String _productLogoSmallName = null;
	private static Image _productLogoSmallImage;
	private static ImageIcon _productLogoSmallImageIcon;

	/** Product logo name (large) */
	private static String _productLogoLargeName = null;
	private static Image _productLogoLargeImage = null;

	//
	// Product License
	private static String _productLicenseURL = null;
	private static final String DEFAULT_ProductLicenseResourceName = "org/adempiere/license.html";

	private static String _productLicenseResourceName = DEFAULT_ProductLicenseResourceName;

	/** Logging */
	private static final transient Logger logger = LogManager.getLogger(Adempiere.class);

	private ApplicationContext applicationContext;

	public static final ApplicationContext getSpringApplicationContext()
	{
		return get().applicationContext;
	}

	/**
	 * Inject the application context from outside <b>and</b> enable {@link Services} to retrieve service implementations from it.
	 * 
	 * Currently seems to be required because currently the client startup procedure needs to be decomposed more.
	 * See <code>SwingUIApplication</code> to know what I mean.
	 *
	 * @param applicationContext
	 */
	public void setApplicationContext(final ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
		logger.info("Set application context: {}", applicationContext);

		// gh #427: NOTE: the "Services.setExternalServiceImplProvider" is not called here because it might introduce a deadlock.
		// we will call it when the spring context was loaded.
	}

	/**
	 * Allows to "statically" autowire a bean that is somehow not wired by spring. Needs the applicationContext to be set.
	 *
	 * @param bean
	 */
	public static final void autowire(final Object bean)
	{
		final ApplicationContext springApplicationContext = getSpringApplicationContext();
		if (springApplicationContext == null)
		{
			return;
		}
		springApplicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
	}

	/**
	 * When running this method from within a junit test, we need to fire up spring
	 * 
	 * @param requiredType
	 * @return
	 */
	public static final <T> T getBean(final Class<T> requiredType)
	{
		final ApplicationContext springApplicationContext = getSpringApplicationContext();
		throwExceptionIfNull(springApplicationContext);

		return springApplicationContext.getBean(requiredType);
	}

	private static void throwExceptionIfNull(final ApplicationContext springApplicationContext)
	{
		if (springApplicationContext != null)
		{
			return;
		}
		final String message;
		if (isUnitTestMode())
		{
			message = "This unit test requires a spring ApplicationContext; A known way to do that is to annotate the test class like this:\n"
					+ "\n"
					+ "@RunWith(SpringRunner.class)\n"
					+ "@SpringBootTest(classes = { StartupListener.class, <further configuration classes> })\n"
					+ "public class YourTest ...\n"
					+ "\n"
					+ "Where the further configuration classes contain @ComponentScann annotations to discover spring components required by the actual tests"
					+ "Also see https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html";
		}
		else
		{
			message = "SpringApplicationContext not configured yet";
		}
		throw new IllegalStateException(message);
	}

	//
	// Load product name, versions etc from resource file
	static
	{
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		final String VERSION_RES = "org/adempiere/version.properties";
		InputStream inputStream = loader.getResourceAsStream(VERSION_RES);
		if (inputStream != null)
		{
			Properties properties = new Properties();
			try
			{
				properties.load(inputStream);
				_mainVersion = properties.getProperty("MAIN_VERSION", _mainVersion);
				_dateVersion = properties.getProperty("DATE_VERSION", _dateVersion);
				_databaseVersion = properties.getProperty("DB_VERSION", _databaseVersion);
				//
				_implementationVendor = properties.getProperty("IMPLEMENTATION_VENDOR", _implementationVendor);
				_implementationVersion = properties.getProperty("IMPLEMENTATION_VERSION", _implementationVersion);
				//
				_productName = properties.getProperty("product.name", _productName);
				_subTitle = properties.getProperty("product.subtitle", _subTitle);
				_productUrl = properties.getProperty("product.url");
				_copyright = properties.getProperty("product.copyright");
				_brandCopyright = properties.getProperty("product.brand");
				_onlineHelpUrl = properties.getProperty("product.support.url", _onlineHelpUrl);
				_supportEmail = properties.getProperty("product.support.email", _supportEmail);
				//
				// Icons:
				_productIconSmallName = properties.getProperty("product.icon.small");
				_productLogoSmallName = properties.getProperty("product.logo.small");
				_productLogoLargeName = properties.getProperty("product.logo.large");
				//
				// License
				_productLicenseURL = properties.getProperty("product.license.url");
				_productLicenseResourceName = properties.getProperty("product.license.resource", _productLicenseResourceName);
			}
			catch (IOException e)
			{
				// NOTE: logger is not available yet
				System.err.println("Error while loading " + VERSION_RES + ": " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		else
		{
			// NOTE: logger is not available yet
			System.err.println("Resource not found: " + VERSION_RES);
		}
	}

	private Adempiere()
	{
		super();
	};

	public static String getMainVersion()
	{
		return _mainVersion;
	}

	public static String getDateVersion()
	{
		return _dateVersion;
	}

	public static String getDatabaseVersion()
	{
		return _databaseVersion;
	}

	/**
	 * @return product's name
	 */
	public static String getName()
	{
		return _productName;
	}   // getName

	/**
	 * @return product name's subtitle (e.g. the motto)
	 */
	public static String getSubtitle()
	{
		return _subTitle;
	}   // getSubitle

	/**
	 * @return product's full URL
	 */
	public static String getURL()
	{
		return _productUrl;
	}   // getURL

	/**
	 * @return product's copyright text
	 */
	public static String getCopyright()
	{
		return _copyright;
	}

	/**
	 * @return product's brand copyright
	 */
	public static String getBrandCopyright()
	{
		return _brandCopyright;
	}

	/**
	 * Get Product Version
	 *
	 * @return Application Version
	 */
	public static String getVersion()
	{
		return getMainVersion() + " @ " + getDateVersion();
	}   // getVersion

	/**
	 * Summary (Windows). Adempiere(tm) Version 2.5.1a_2004-03-15 - Smart ERP & CRM - Copyright (c) 1999-2005 Jorg Janke; Implementation: 2.5.1a 20040417-0243 - (C) 1999-2005 Jorg Janke, Adempiere
	 * Inc. USA
	 *
	 * @return Summary in Windows character set
	 */
	public static String getSummary()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getName()).append(" ")
				.append(getMainVersion()).append("_").append(getDateVersion())
				.append(" -").append(getSubtitle())
				.append("- ").append(getCopyright())
				.append("; Implementation: ").append(getImplementationVersion())
				.append(" - ").append(getImplementationVendor());
		return sb.toString();
	}	// getSummary

	/**
	 * Set Package Info
	 */
	private static void setPackageInfo()
	{
		if (_implementationVendor != null)
			return;

		Package adempierePackage = Package.getPackage("org.compiere");
		_implementationVendor = adempierePackage.getImplementationVendor();
		_implementationVersion = adempierePackage.getImplementationVersion();
		if (_implementationVendor == null)
		{
			_implementationVendor = "Supported by community";
			_implementationVersion = "ERP";
		}
	}	// setPackageInfo

	/**
	 * Get Jar Implementation Version
	 *
	 * @return Implementation-Version
	 */
	public static String getImplementationVersion()
	{
		if (_implementationVersion == null)
			setPackageInfo();
		return _implementationVersion;
	}	// getImplementationVersion

	/**
	 * Get Jar Implementation Vendor
	 *
	 * @return Implementation-Vendor
	 */
	public static String getImplementationVendor()
	{
		if (_implementationVendor == null)
			setPackageInfo();
		return _implementationVendor;
	}	// getImplementationVendor

	/**
	 * Get Checksum
	 *
	 * @return checksum
	 */
	public static int getCheckSum()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getName()).append(" ").append(getMainVersion()).append(getSubtitle());
		return sb.toString().hashCode();
	}   // getCheckSum

	/**
	 * Summary in ASCII
	 *
	 * @return Summary in ASCII
	 */
	public static String getSummaryAscii()
	{
		String retValue = getSummary();
		// Registered Trademark
		retValue = Util.replace(retValue, "\u00AE", "(r)");
		// Trademark
		retValue = Util.replace(retValue, "\u2122", "(tm)");
		// Copyright
		retValue = Util.replace(retValue, "\u00A9", "(c)");
		// Cr
		retValue = Util.replace(retValue, Env.NL, " ");
		retValue = Util.replace(retValue, "\n", " ");
		return retValue;
	}	// getSummaryAscii

	/**
	 * Get Java VM Info
	 *
	 * @return VM info (e.g. Java HotSpot(TM) 64-Bit Server VM 1.7.0_21/23.21-b01)
	 */
	public static String getJavaInfo()
	{
		return System.getProperty("java.vm.name") // e.g. Java HotSpot(TM) 64-Bit Server VM
				+ " " + System.getProperty("java.version") // e.g. 1.7.0_21
				+ "/" + System.getProperty("java.vm.version") // e.g. 23.21-b01
		;
	}	// getJavaInfo

	/**
	 * Get Operating System Info
	 *
	 * @return OS info (e.g. Windows 7 6.1 Service Pack 1)
	 */
	public static String getOSInfo()
	{
		return System.getProperty("os.name") // e.g. Windows 7
				+ " " + System.getProperty("os.version") // e.g. 6.1
				+ " " + System.getProperty("sun.os.patch.level") // e.g. Service Pack 1
		;
	}	// getJavaInfo

	/**
	 * @return URL
	 */
	public static String getOnlineHelpURL()
	{
		return _onlineHelpUrl;
	}

	/**
	 * @return product icon (small, i.e. 16x16 image)
	 */
	public static Image getProductIconSmall()
	{
		if (_productIconSmall == null && !Check.isEmpty(_productIconSmallName, true))
		{
			URL url = org.compiere.Adempiere.class.getResource(_productIconSmallName);
			if (url == null)
				return null;
			final Toolkit tk = Toolkit.getDefaultToolkit();
			_productIconSmall = tk.getImage(url);
		}
		return _productIconSmall;
	}

	/**
	 * @return product's logo (large, high resolution)
	 */
	public static Image getProductLogoLarge()
	{
		if (_productLogoLargeImage == null && !Check.isEmpty(getProductLogoLargeResourceName(), true))
		{
			URL url = getProductLogoLargeURL();
			if (url == null)
				return null;
			Toolkit tk = Toolkit.getDefaultToolkit();
			_productLogoLargeImage = tk.getImage(url);
		}
		return _productLogoLargeImage;
	}   // getImageLogoSmall

	/**
	 * @return product large logo resource URL
	 */
	public static URL getProductLogoLargeURL()
	{
		return org.compiere.Adempiere.class.getResource(getProductLogoLargeResourceName());
	}

	public static String getProductLogoLargeResourceName()
	{
		return _productLogoLargeName;
	}

	/**
	 * @return product's logo (small)
	 */
	public static Image getProductLogoSmall()
	{
		if (_productLogoSmallImage == null && !Check.isEmpty(_productLogoSmallName, true))
		{
			URL url = org.compiere.Adempiere.class.getResource(_productLogoSmallName);
			if (url == null)
				return null;
			Toolkit tk = Toolkit.getDefaultToolkit();
			_productLogoSmallImage = tk.getImage(url);
		}
		return _productLogoSmallImage;
	}   // getImageLogo

	/**
	 * @return Image Icon (100x30 ImageIcon)
	 */
	public static ImageIcon getProductLogoAsIcon()
	{
		if (_productLogoSmallImageIcon == null && getProductLogoSmall() != null)
		{
			_productLogoSmallImageIcon = new ImageIcon(getProductLogoSmall());
		}
		return _productLogoSmallImageIcon;
	}   // getImageIconLogo

	public static String getProductLicenseURL()
	{
		return _productLicenseURL;
	}

	public static String getProductLicenseResourceName()
	{
		return _productLicenseResourceName;
	}

	/**
	 * Get default (Home) directory
	 *
	 * @return Home directory
	 */
	public static String getMetasfreshHome()
	{
		return Ini.getMetasfreshHome();
	}

	/**
	 * Get Support Email
	 *
	 * @return Support mail address
	 */
	public static String getSupportEMail()
	{
		return _supportEmail;
	}   // getSupportEMail

	/**
	 * Set to true if startup() was successfully executed.
	 */
	private volatile boolean started = false;

	/**
	 * ADempiere can be started in different global run modes. The current run mode can be accessed from {@link Ini#getRunMode()} so that e.g. modules can decide which parts for start. Example (async
	 * processors are only started in {@code BACKEND} mode).
	 * <p>
	 * Notes:
	 * <ul>
	 * <li>"UniTestMode" mode is not a runmode because the system can be unit tested beeing in every run mode</li>
	 * <li>This concept might be refined further (e.g. multiple runmodes), and might be combined with the functionality of {@link ModelValidationEngine#CTX_InitEntityTypes}</li>
	 * </ul>
	 *
	 * @task 04585
	 * @see Ini#getRunMode()
	 */
	public enum RunMode
	{
		/**
		 * Indicates that ADempiere is running as a user-local swing client
		 */
		SWING_CLIENT,

		/**
		 * Indicates that ADempiere is running as a server-side webUI, but without back-end services (i.e. without the serverRoot servlets) Note that if the webUI and the back-end services are running
		 * within one JVM or one ear, then the global run mode will be {@code BACKEND}.
		 */
		WEBUI,

		/**
		 * Indicates that ADempiere is running as a (server-side) back-end service.
		 */
		BACKEND
	}

	/**
	 * Immediately returns if the member <code>started</code> is already true. Also calls {@link #startupEnvironment(RunMode)}, <b>unless the given runMode is {@link RunMode#SWING_CLIENT}</b>
	 *
	 * @param runMode
	 * @deprecated consider using {@link MetasfreshBoot} spring configuration
	 */
	@Deprecated
	public synchronized void startup(@NonNull final RunMode runMode)
	{
		if (started)
		{
			return;
		}
		
		Metasfresh.startupWithoutSpring(runMode);

		started = true;
	}   // startup

	/**
	 * If enabled, everything will run database decoupled. Supposed to be called before an interface like org.compiere.model.I_C_Order is to be used in a unit test.
	 */
	public static void enableUnitTestMode()
	{
		unitTestMode = true;
	}

	public static boolean isUnitTestMode()
	{
		return unitTestMode;
	}

	private static boolean unitTestMode = false;
}	// Adempiere
