/*
 *  Copyright 2008 bbossgroups
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.frameworkset.security.session.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.frameworkset.security.session.InvalidateCallback;
import org.frameworkset.security.session.Session;
import org.frameworkset.security.session.SessionBasicInfo;
import org.frameworkset.security.session.SessionUtil;
import org.frameworkset.security.session.domain.App;
import org.frameworkset.security.session.domain.CrossDomain;

import com.frameworkset.util.StringUtil;

/**
 * <p>Title: SessionHttpServletRequestWrapper.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2014年4月30日
 * @author biaoping.yin
 * @version 3.8.0
 */
public class SessionHttpServletRequestWrapper extends HttpServletRequestWrapper implements InvalidateCallback {
	private static Logger log = Logger.getLogger(SessionHttpServletRequestWrapper.class);
	private String sessionid;
	private HttpSessionImpl session;
	private HttpServletResponse response;
	private ServletContext servletContext;	
	private String appkey ;
	private boolean usewebsession = true;
	public SessionHttpServletRequestWrapper(HttpServletRequest request,HttpServletResponse response,ServletContext servletContext) {
		super(request);
		try
		{
			SessionUtil.init(SessionUtil.getAppKeyFromRequest(this));
			if(SessionUtil.getSessionManager() != null)
			{
				sessionid = StringUtil.getCookieValue((HttpServletRequest)request, SessionUtil.getSessionManager().getCookiename());
				usewebsession = SessionUtil.getSessionManager().usewebsession();
				if( !usewebsession)
					appkey = SessionUtil.getAppKey(this);
			}
		}
		catch(Throwable e)
		{
			log.debug("Init bboss session failed:",e);
		}
		this.servletContext = servletContext;
		this.response = response;
		
	}

	@Override
	public HttpSession getSession() {
		 return getSession(true);
	}
	/**
	 * 清除session数据
	 * @param sessionid
	 */
	public void removeSession(String sessionid)
	{
		if( usewebsession)
			return;
		if(this.sessionid != null && this.sessionid.equals(sessionid))
		{
			 
			HttpSession session  = getSession(false);
			if(session != null)
				session.invalidate();
		}
		else
		{
			HttpSession session = _getSession( sessionid);
			if(session != null)
				session.invalidate();
		}
	}

	
	private String getRequestUrl()
	{
		StringBuilder basePath = new StringBuilder().append(getScheme()).append("://").append(getServerName());
		if(getServerPort() != 80)
			basePath.append(":").append(getServerPort() ) ;
		 
		basePath
			.append( this.getRequestURI());
		if(this.getQueryString() != null)
			basePath.append("?").append(this.getQueryString());
		return basePath.toString();
	}
	@Override
	public HttpSession getSession(boolean create) {
		if( usewebsession)
		{
			// TODO Auto-generated method stub
			return super.getSession(create);
		}
		if(sessionid == null)
		{
			if(create)
			{

//				String appkey = SessionUtil.getAppKey(this);
				SessionBasicInfo sessionBasicInfo = new SessionBasicInfo();
				sessionBasicInfo.setAppKey(appkey);
				sessionBasicInfo.setReferip(StringUtil.getClientIP(this));
				sessionBasicInfo.setRequesturi(this.getRequestUrl());
				
				this.session = (HttpSessionImpl) SessionUtil.createSession(servletContext,sessionBasicInfo,this.getContextPath(),this);				
				sessionid = session.getId();
				 

				writeCookies( );
				return this.session;
			}
			else
			{
				return null;
			}
		}
		else if(session != null)
		{
			return session;
		}
		else
		{
//			String appkey =  SessionUtil.getAppKey(this);

			Session session = SessionUtil.getSession(appkey,this.getContextPath(),sessionid);
			if(session == null)//session不存在，创建新的session
			{				
				if(create)
				{

				
					
					SessionBasicInfo sessionBasicInfo = new SessionBasicInfo();
					sessionBasicInfo.setAppKey(appkey);
					sessionBasicInfo.setReferip(StringUtil.getClientIP(this));
					sessionBasicInfo.setRequesturi(this.getRequestUrl());
					
					this.session = (HttpSessionImpl) SessionUtil.createSession(servletContext,sessionBasicInfo,this.getContextPath(),this);
					sessionid = session.getId();
					

					writeCookies( );
				}
			}
			else
			{
				this.session =  new HttpSessionImpl(session,servletContext,this.getContextPath(),this);
			}
			return this.session;
		}
		
		
	}
	
	
	private HttpSession _getSession(String sessionid) {
		if( usewebsession)
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
//			String appkey =  SessionUtil.getAppKey(this);

			Session session_ = SessionUtil.getSession(appkey,this.getContextPath(),sessionid);
			if(session_ == null)//session不存在，创建新的session
			{				
				return null;
			}
			else
			{
				session =  new HttpSessionImpl(session_,servletContext,this.getContextPath(),null);
			}
			return session;
		}
		
		
	}

	public void touch() {
		if( usewebsession)
			return;
		if(this.sessionid != null )
		{
			if(session == null)
			{
//				String appkey =  SessionUtil.getAppKey(this);
				Session session_ = SessionUtil.getSession(appkey,this.getContextPath(), sessionid);
				if(session_ == null || !session_.isValidate())
				{
					this.sessionid = null;
					return;
				}
				this.session =  new HttpSessionImpl(session_,servletContext,this.getContextPath(),this);
			}
			if(session != null && !session.isNew() )
			{
				session.touch(this.getRequestUrl());
			}
		}
		
	}
	private static Object dummy = new Object();
	private void writeCookies( )
	{
		int cookielivetime = -1;
		CrossDomain crossDomain = SessionUtil.getSessionManager().getCrossDomain() ;
		if(crossDomain == null)
		{
			boolean secure = SessionUtil.getSessionManager().isSecure();
			if(!this.isSecure())
				secure = false;
			StringUtil.addCookieValue(this, response, SessionUtil.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionUtil.getSessionManager().isHttpOnly(),
					secure,SessionUtil.getSessionManager().getDomain());
		}
		else
		{
			String currentDomain = this.getServerName();
			if(!currentDomain.equals(crossDomain.getRootDomain()) && !currentDomain.endsWith("."+crossDomain.getRootDomain()))//非跨域访问，则直接写应用的session cookieid,解决通过非共享域方式无法访问系统的问题
			{
				boolean secure = SessionUtil.getSessionManager().isSecure();
				if(!this.isSecure())
					secure = false;
				StringUtil.addCookieValue(this, response, SessionUtil.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionUtil.getSessionManager().isHttpOnly(),
						secure,SessionUtil.getSessionManager().getDomain());
				return;
			}
			List<App> apps = crossDomain.getDomainApps();
			if(crossDomain.get_paths() != null)
			{
				boolean secure = SessionUtil.getSessionManager().isSecure();
				if(!this.isSecure())
					secure = false;
				for(String path:crossDomain.get_paths())
				{
					StringUtil.addCookieValue(this, path,
												response, 
												SessionUtil.getSessionManager().getCookiename(), 
												sessionid, cookielivetime,
												SessionUtil.getSessionManager().isHttpOnly(),								
												secure,
												crossDomain.getRootDomain());
				}
			}
			else
			{
				boolean secure = SessionUtil.getSessionManager().isSecure();
				if(!this.isSecure())
					secure = false;
				Map<String,Object> setted = new HashMap<String,Object>();
				for(App app:apps)
				{
					if(app.getPath() == null)
					{
						StringUtil.addCookieValue(this, response, SessionUtil.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionUtil.getSessionManager().isHttpOnly(),
								secure,crossDomain.getRootDomain());
					}
					else
					{
						if(!setted.containsKey(app.getPath()))
						{
							StringUtil.addCookieValue(this, app.getPath(),response, SessionUtil.getSessionManager().getCookiename(), sessionid, cookielivetime,SessionUtil.getSessionManager().isHttpOnly(),								
									secure,crossDomain.getRootDomain());
							setted.put(app.getPath(), dummy);
						}
						else
						{
							
						}
						
						
					}
				}
				setted = null;
			}
			
		}
	}

	@Override
	public String getRequestedSessionId() {
		if( usewebsession)
		{
			return super.getRequestedSessionId();
		}
		if(this.sessionid != null)
			return sessionid;
		HttpSession session = this.getSession(false);
		if(session == null)
			return null;
		else
			return session.getId();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		if( usewebsession)
		{
			return super.isRequestedSessionIdFromCookie();
		}
		return true;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		if( usewebsession)
		{
			return super.isRequestedSessionIdFromURL();
		}
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		if( usewebsession)
		{
			return super.isRequestedSessionIdFromUrl();
		}
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		if( usewebsession)
		{
			return super.isRequestedSessionIdValid();
		}
		HttpSessionImpl session = (HttpSessionImpl)this.getSession(false);
		if(session == null)
			return false;
		else
			return session.getInnerSession().isValidate();
	}

	@Override
	public void invalidateCallback() {
		this.session = null;
		this.sessionid = null;
		
	}
	
	public void submit()
	{
		if(session != null && session.islazy())
		{			
			session.submit();			
		}
	}

}
