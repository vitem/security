package org.frameworkset.security.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.frameworkset.security.session.domain.CrossDomain;
import org.frameworkset.security.session.impl.HttpSessionImpl;
import org.frameworkset.security.session.impl.SessionEventImpl;
import org.frameworkset.security.session.impl.SessionHttpServletRequestWrapper;
import org.frameworkset.security.session.impl.SessionManager;
import org.frameworkset.security.session.statics.AttributeInfo;
import org.frameworkset.security.session.statics.NullSessionStaticManagerImpl;
import org.frameworkset.security.session.statics.SessionConfig;
import org.frameworkset.security.session.statics.SessionStaticManager;
import org.frameworkset.spi.BaseApplicationContext;
import org.frameworkset.spi.DefaultApplicationContext;
import org.frameworkset.spi.SPIException;

import com.frameworkset.util.SimpleStringUtil;
import com.frameworkset.util.StringUtil;

public class SessionUtil {
	private static Logger log = Logger.getLogger(SessionUtil.class);
	private static SessionManager sessionManager;
	private static SessionStaticManager sessionStaticManager;
 
	private static boolean inited = false;
	private static final String dianhaochar = "____";
	private static final String moneychar = "_____";
	private static final int msize = moneychar.length();
	public SessionUtil() {
		// TODO Auto-generated constructor stub
	}
	
	public static void init(String contextpath){
		if(inited)
			return ;
		synchronized(SessionUtil.class)
		{
			if(inited)
				return ;
			try
			{
				BaseApplicationContext context = DefaultApplicationContext.getApplicationContext("sessionconf.xml");
				SessionManager sessionManager = context.getTBeanObject("sessionManager", SessionManager.class);
				SessionStaticManager sessionStaticManager = null;
				String monitorScope = null;
				if(sessionManager != null)
				{
					if(!sessionManager.usewebsession())
					{
						sessionStaticManager = context.getTBeanObject("sessionStaticManager", SessionStaticManager.class);
						monitorScope = sessionStaticManager.getMonitorScope();
					}
					else
						sessionStaticManager = new NullSessionStaticManagerImpl();
					if(monitorScope == null)
						monitorScope = SessionStaticManager.MONITOR_SCOPE_SELF;
					sessionManager.initSessionConfig(contextpath,monitorScope);
					SessionUtil.sessionManager = sessionManager;
					SessionUtil.sessionStaticManager = sessionStaticManager;
				}
			}
			catch(SPIException e)
			{
				log.info("初始化bboss session结果:"+e.getMessage());
				
			}
			finally
			{
				inited = true;
			}
		}
		
	}
	
	public static Object convertValue(String value,AttributeInfo attributeInfo)
	{
		if(attributeInfo.getType().equals("String"))
			return value;
		else if(attributeInfo.getType().equals("int"))
			return Integer.parseInt(value);
		else if(attributeInfo.getType().equals("long"))
			return Long.parseLong(value);
		else if(attributeInfo.getType().equals("double"))
			return Double.parseDouble(value);
		else if(attributeInfo.getType().equals("float"))
			return Float.parseFloat(value);
		else if(attributeInfo.getType().equals("boolean"))
			return Boolean.parseBoolean(value);
		return value;
	}
	
	public static Map<String,AttributeInfo> parserExtendAttributes(HttpServletRequest request,SessionConfig sessionConfig  )
	{
		AttributeInfo[] monitorAttributeArray = sessionConfig.getExtendAttributeInfos();
		if(monitorAttributeArray == null || monitorAttributeArray.length == 0)
		{
			return null;
		}
		Map<String,AttributeInfo>  datas = new HashMap<String,AttributeInfo>();
		for(AttributeInfo attributeInfo : monitorAttributeArray)
		{
			String value = request.getParameter(attributeInfo.getName());
			if(value != null )
			{
				
				if(value.trim().equals("")  )
				{
					if(attributeInfo.isEnableEmptyValue())
					{
						String enableEmptyValue = request.getParameter(attributeInfo.getName()+"_enableEmptyValue");
						if(enableEmptyValue !=  null)
						{
							try {
								attributeInfo = attributeInfo.clone();
								attributeInfo.setValue("");
								datas.put(attributeInfo.getName(), attributeInfo);
							} catch (CloneNotSupportedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
				}
				else
				{
					try {
						attributeInfo = attributeInfo.clone();
						attributeInfo.setValue(  convertValue(  value,  attributeInfo));
						datas.put(attributeInfo.getName(), attributeInfo);
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		return datas;
	}
	
	public static void evalqueryfields(AttributeInfo[] monitorAttributeArray, Map keys)
	{
		 
		if(monitorAttributeArray != null && monitorAttributeArray.length > 0)
		{
			for(AttributeInfo attr:monitorAttributeArray)
			{
				keys.put(attr.getName(), 1);
				 
				
			}
			
			
		}
		
		
		
	}
	
	public static boolean filter(String key) {
		return key.equals("maxInactiveInterval") || key.equals("creationTime")
				|| key.equals("lastAccessedTime") || key.equals("referip")
				|| key.equals("_validate") || key.equals("sessionid")
				|| key.equals("_id") || key.equals("appKey")
				|| key.equals("host")
				|| key.equals("secure")
				|| key.equals("httpOnly")
				|| key.equals("requesturi")
				|| key.equals("lastAccessedUrl")
				|| key.equals("lastAccessedHostIP");
			
	}
	
	public static String recoverSpecialChar(String attribute) {
		if (attribute.startsWith(moneychar)) {
			attribute = "$" + attribute.substring(msize);
		}

		attribute = attribute.replace(dianhaochar, ".");
		return attribute;
	}
	public static String converterSpecialChar(String attribute)
	{
		attribute = attribute.replace(".", dianhaochar);
		if(attribute.startsWith("$"))
		{
			if(attribute.length() == 1)
			{
				attribute = moneychar;
			}
			else
			{
				attribute = moneychar + attribute.substring(1);
			}
		}
		return attribute;
	}
	
	public static boolean haveSessionListener() 
	{
		return sessionManager.haveSessionListener();
	}

	public static Session getSession(String appkey,String contextPath, String sessionid) {
		// TODO Auto-generated method stub
		return sessionManager.getSessionStore().getSession(appkey,contextPath, sessionid);
	}
	

	public static String serial(Object value)
	{
//		if(value != null)
//		{
//			try {
//				value = ObjectSerializable.toXML(value);
////				value = new String(((String)value).getBytes(Charset.defaultCharset()),"UTF-8");
//			} catch (Exception e) {
//				throw new RuntimeException(e);
//			}
//		}
//		return value;
		return sessionManager.getSessionSerial().serialize(value);
	}
	
	public static Object serial(Object value,String serialType)
	{
//		if(value != null)
//		{
//			try {
//				value = ObjectSerializable.toXML(value);
////				value = new String(((String)value).getBytes(Charset.defaultCharset()),"UTF-8");
//			} catch (Exception e) {
//				throw new RuntimeException(e);
//			}
//		}
//		return value;
		return sessionManager.getSessionSerial(serialType).serialize(value);
	}
	
	public static String getLikeCondition(Object condition,String serialType)
	{
		return sessionManager.getSessionSerial(serialType).handleLikeCondition(condition);
	}
	
	public static Object unserial(String value)
	{
		return sessionManager.getSessionSerial().deserialize(value);
	}
	public static Object unserial(String value,String serialType)
	{
		return sessionManager.getSessionSerial( serialType).deserialize(value);
	}
	public static String wraperAttributeName(String appkey,String contextpath, String attribute)
	{
		CrossDomain crossDomain = sessionManager.getCrossDomain();
		if(crossDomain == null)
			return attribute;
		return crossDomain.wraperAttributeName(appkey, contextpath, attribute);
	}
	
	public static String dewraperAttributeName(String appkey,String contextpath, String attribute)
	{
		CrossDomain crossDomain = sessionManager.getCrossDomain();
		if(crossDomain == null)
			return attribute;
		return crossDomain.dewraperAttributeName(appkey, contextpath, attribute);
	}
	public static String getAppKey(HttpServletRequest request)
	{
		String appcode = getSessionManager().getAppcode();
		if(appcode != null)
		{
			return appcode;
		}
		 return getAppKeyFromRequest(request);
		
	}
	

	public static SessionConfig getSessionConfig(String appcode)
	{
		return sessionManager.getSessionConfig(appcode,true);
	}
	public static SessionConfig getSessionConfig(String appcode,boolean serialattributes)
	{
		return sessionManager.getSessionConfig(appcode, serialattributes);
	}
	
	 
	
	public static void removeSession(String sessionId,HttpServletRequest request)
	{
		if(request instanceof SessionHttpServletRequestWrapper)
		{
			((SessionHttpServletRequestWrapper)request).removeSession(sessionId);
		}
	}
	
	public static void removeSession(String sessionId,String appcode)
	{
		if( getSessionManager().usewebsession())
			return;
		
		HttpSession session = _getSession(appcode, sessionId);
		if(session != null)
			session.invalidate();
	
	}
	private static HttpSession _getSession(String appkey,String sessionid) {
		if(getSessionManager() == null || getSessionManager().usewebsession())
		{
			return null;
		}
		HttpSessionImpl session = null;
		if(sessionid == null)
		{
			
			return session;
		}
		
		else
		{
//			String appkey =  SessionHelper.getAppKey(this);

			Session session_ = getSession(appkey,null,sessionid);
			if(session_ == null)//session不存在，创建新的session
			{				
				return null;
			}
			else
			{
				session =  new HttpSessionImpl(session_,null,null,null);
			}
			return session;
		}
		
		
	}
	
	public static String getAppKeyFromRequest(HttpServletRequest request)
	{
//		String appcode = getSessionManager().getAppcode();
//		if(appcode != null)
//		{
//			return appcode;
//		}
		
		if(request != null)
		{
			String appKey = request.getContextPath().replace("/", "");
			if(appKey.equals(""))
				appKey = "ROOT";
			return appKey;
		}
		return null;
		
	}
	
	public static String getAppKeyFromServletContext(ServletContext context)
	{
//		String appcode = getSessionManager().getAppcode();
//		if(appcode != null)
//		{
//			return appcode;
//		}
		
		if(context != null)
		{
			
			String appKey = context.getContextPath().replace("/", "");
			if(appKey.equals(""))
				appKey = "ROOT";
			return appKey;
		}
		return null;
		
	}
	
	public static boolean hasMonitorPermission(String app, HttpServletRequest request)
	{
		return getSessionStaticManager().hasMonitorPermission(app, request);
	}
	
	public static boolean hasDeleteAppPermission(String app, HttpServletRequest request)
	{
		return getSessionStaticManager().hasMonitorPermission(app, request);
	}
	
	public static boolean deleteApp(String app) throws Exception
	{
		return getSessionStaticManager().deleteApp(app);
	}
	
	public static boolean isMonitorAll() throws Exception
	{
		return getSessionStaticManager().isMonitorAll();
	}
	/**
	 * 
	 * @param monitorAttributes
	 * @return
	 */
	public static  AttributeInfo[] getExtendAttributeInfos(String monitorAttributes)
	{
		
		 if(StringUtil.isEmpty(monitorAttributes))
			 return null;
		AttributeInfo[] monitorAttributeArray = SimpleStringUtil.json2Object(monitorAttributes,AttributeInfo[].class);
//		 AttributeInfo[] monitorAttributeArray = null;
//		if(!StringUtil.isEmpty(monitorAttributes))
//		{
//			String[] monitorAttributeArray_ = monitorAttributes.split(",");
//			monitorAttributeArray = new AttributeInfo[monitorAttributeArray_.length];
//			AttributeInfo attributeInfo = null;
//			for(int i = 0; i < monitorAttributeArray_.length; i ++)
//			{
//				String attr = monitorAttributeArray_[i];
//				attributeInfo = new AttributeInfo();
//				String attrinfo[] = attr.split(":");
//				if(attrinfo.length > 2)
//				{
//					attributeInfo.setName(attrinfo[0]);
//					attributeInfo.setType(attrinfo[1]);
//					attributeInfo.setCname(attrinfo[2]);
//				}
//				else if(attrinfo.length > 1)
//				{
//					attributeInfo.setName(attrinfo[0]);
//					attributeInfo.setType(attrinfo[1]);
//				}
//				else
//				{
//					attributeInfo.setName(attrinfo[0]);
//					attributeInfo.setType("String");
//				}
//				monitorAttributeArray[i]=attributeInfo;
//					
//				
//			}
//			
//		}
		return monitorAttributeArray;
	}
	public static List<AttributeInfo> evalqueryfiledsValue(AttributeInfo[] attributeInfos, List<String> data, int offset,String serialType) {
		List<AttributeInfo> extendAttrs = null;
		 
		if(attributeInfos != null && attributeInfos.length > 0)				
		{
			extendAttrs = new ArrayList<AttributeInfo>();
			AttributeInfo attrvalue = null;
			for(int j = 0; j < attributeInfos.length; j ++ )
			{
				AttributeInfo attributeInfo = attributeInfos[j];
				try {
					attrvalue = attributeInfo.clone();
					String value = data.get(j +offset);
					attrvalue.setValue(unserial(  value,serialType));
					extendAttrs.add(attrvalue);
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return extendAttrs;
	}
	
	public static void destroy() {
		sessionManager = null;
		
	}


	public static SessionManager getSessionManager() {
		return sessionManager;
	}
	
	public static SessionStaticManager getSessionStaticManager()
	{
		return sessionStaticManager;
	}
	
	public static HttpSession createSession(ServletContext servletContext,SessionBasicInfo sessionBasicInfo,String contextpath,InvalidateCallback invalidateCallback)
	{
		HttpSession session = sessionManager.getSessionStore().createHttpSession(   servletContext,  sessionBasicInfo,  contextpath,  invalidateCallback);
		
		return session;
	}
	public static void dispatchEvent(SessionEventImpl sessionEvent) 
	{
		sessionManager.dispatchEvent(sessionEvent);
	}
	
	

}
