/**
 * 
 */
package com.hyk.proxy.plugin.phptunnel.event;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hyk.proxy.framework.event.HttpProxyEvent;
import com.hyk.proxy.framework.event.tunnel.AbstractTunnelProxyEventService;
import com.hyk.proxy.framework.security.SimpleEncrypter;
import com.hyk.proxy.framework.security.SimpleSecurityService;
import com.hyk.proxy.framework.util.ListSelector;


/**
 * @author wqy
 * 
 */
class PhpTunnelClientProxyEventService extends AbstractTunnelProxyEventService
{
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private ClientSocketChannelFactory factory;

	private int tunnelSourcePort = 4810;
	private ExecutorService executor;
	private ListSelector<URL> selector;
	private URL url;
	private String seesionId;

	private SimpleSecurityService encrpt = new SimpleSecurityService();
	private ChannelDownstreamHandler encrypter = null;
	private ChannelUpstreamHandler decrypter = null;

	private static AtomicInteger seed = new AtomicInteger(1);

	public PhpTunnelClientProxyEventService(ExecutorService executor,
	        int localServerPort, ListSelector<URL> selector)
	{
		this.executor = executor;
		this.tunnelSourcePort = localServerPort;
		this.selector = selector;
		this.factory = new NioClientSocketChannelFactory(executor, executor);
		this.url = selector.select();
	}

	private static String generateSessionID()
	{
		int i = seed.getAndIncrement();
		String sessionid = String.format("%010d", i);
		return sessionid;
	}

	protected String getRemoteAddress(HttpRequest request)
	{
		String host = request.getHeader("Host");
		if (null == host)
		{
			String url = request.getUri();
			if (url.startsWith("http://"))
			{
				url = url.substring(7);
				int next = url.indexOf("/");
				host = url.substring(0, next);
			}
			else
			{
				host = url;
			}
		}
		int index = host.indexOf(":");
		int port = 80;
		if (request.getMethod().equals(HttpMethod.CONNECT))
		{
			port = 443;
		}
		String hostValue = host;
		if (index > 0)
		{
			hostValue = host.substring(0, index).trim();
			port = Integer.parseInt(host.substring(index + 1).trim());
		}
		if (logger.isDebugEnabled())
		{
			logger.debug("Get remote address " + hostValue + ":" + port);
		}
		return hostValue + ":" + port;
	}

	@Override
	protected boolean needForwardConnect()
	{
		return false;
	}

	@Override
	protected boolean forceShortConnection()
	{
		return false;
	}

	@Override
	protected ChannelDownstreamHandler getEncrypter()
	{
		return encrypter;
	}

	@Override
	protected ChannelUpstreamHandler getDecrypter()
	{
		return decrypter;
	}

	@Override
	protected void getRemoteChannel(HttpProxyEvent event, final CallBack callback)
	{
		try
		{
			if (remoteChannel != null && remoteChannel.isConnected())
			{
				callback.callback(remoteChannel);
			}
			
			// hconn.setRequestMethod("POST");
			if (isHttps)
			{
				encrypter = new SimpleEncrypter.SimpleEncryptEncoder();
				decrypter = new SimpleEncrypter.SimpleDecryptDecoder();
				seesionId = generateSessionID();
				if(logger.isDebugEnabled())
				{
					logger.debug("Generate https session ID:" + seesionId);
				}

				final URLConnection conn = url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);

				if (event.getSource() instanceof HttpRequest)
				{

					HttpRequest req = (HttpRequest) event.getSource();
					String target = new String(encrpt.encrypt(getRemoteAddress(req)
					        .getBytes()));
					conn.setRequestProperty("TunnelTarget", target);
					
				}
				final HttpURLConnection hconn = (HttpURLConnection) conn;
				conn.setRequestProperty("TunnelSource", InetAddress
				        .getLocalHost().getHostAddress()
				        + ":"
				        + tunnelSourcePort);
				conn.setRequestProperty("TunnelSessionId", seesionId);
				PhpTunnelLocalServerHandler.registerCallBack(seesionId,
				        callback);

				final String tempId = seesionId;

				executor.submit(new Runnable()
				{
					public void run()
					{

						byte[] buf = new byte[4096];
						try
						{
							hconn.connect();
							if (hconn.getResponseCode() != 200)
							{
								close();
								return;
							}
							int len = conn.getInputStream().read(buf);
							close();
						}
						catch (Exception e)
						{
							logger.error("Failed!", e);
						}
						finally
						{
							PhpTunnelLocalServerHandler.removeCallBack(tempId);

						}

					}
				});
			}
			else
			{
				if (event.getSource() instanceof HttpRequest)
				{

					String host = url.getHost();
					int port = url.getPort();
					if (port == -1)
					{
						port = 80;
					}
					ChannelPipeline pipeline = pipeline();
					pipeline.addLast("empty", new EmptyHandler());
					remoteChannel = factory.newChannel(pipeline);
					final Channel ch = remoteChannel;
					remoteChannel.connect(new InetSocketAddress(host, port)).addListener(new ChannelFutureListener()
					{
						@Override
						public void operationComplete(ChannelFuture future) throws Exception
						{
							callback.callback(ch);
						}
					});
					
				}
			}

		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ChannelBuffer encrypt(ChannelBuffer content)
	{
		ByteBuffer raw = content.toByteBuffer();
		raw = encrpt.encrypt(raw);
		return ChannelBuffers.wrappedBuffer(raw);
	}
	
	private ChannelBuffer decrypt(ChannelBuffer content)
	{
		ByteBuffer raw = content.toByteBuffer();
		raw = encrpt.decrypt(raw);
		return ChannelBuffers.wrappedBuffer(raw);
	}
	
	@Override
	protected MessageEvent preProcessForwardMessageEvent(MessageEvent event)
	{
		if (event.getMessage() instanceof HttpResponse)
		{
			
			HttpResponse res = (HttpResponse) event.getMessage();
			if(logger.isDebugEnabled())
			{
				logger.debug("Recv response:" + res);
			}
			
			ChannelBuffer content = res.getContent();
			if(res.getStatus().getCode() >= 300)
			{
				if(logger.isDebugEnabled())
				{
					logger.debug("Recv error content:" + new String(content.toByteBuffer().array()));
				}
				return event;
			}
			if(content != null)
			{		
				content = decrypt(content);
				return new UpstreamMessageEvent(event.getChannel(), content, event.getRemoteAddress());
			}
		}
		else if (event.getMessage() instanceof HttpChunk)
		{
			HttpChunk chunk = (HttpChunk) event.getMessage();
			ChannelBuffer content = chunk.getContent();
			return new UpstreamMessageEvent(event.getChannel(), decrypt(content), event.getRemoteAddress());
		}
		return event;
	}
	
	@Override
	protected HttpProxyEvent preProcessForwardHttpProxyEvent(
	        HttpProxyEvent event)
	{
		switch (event.getType())
		{
			case RECV_HTTP_REQUEST:
			{
				HttpRequest recvReq = (HttpRequest) event.getSource();
				String host = url.getHost();
				int port = url.getPort();
				if (port == -1)
				{
					port = 80;
				}
				HttpRequest newReq = new DefaultHttpRequest(
				        HttpVersion.HTTP_1_1, HttpMethod.POST,
				        url.toString());

				newReq.setHeader("Host", host + ":" + port);
				StringBuffer headerBuf = new StringBuffer();
				headerBuf.append(recvReq.getMethod().toString())
				        .append(" ").append(recvReq.getUri()).append(" ")
				        .append(recvReq.getProtocolVersion().toString())
				        .append("\r\n");
				recvReq.removeHeader("Proxy-Connection");
				newReq.addHeader("TunnelTarget", encrpt.encrypt(getRemoteAddress(recvReq)));
				recvReq.setHeader("Connection", "close");
				Set<String> headers = recvReq.getHeaderNames();
				for (String headerName : headers)
				{
					List<String> headerValues = recvReq
					        .getHeaders(headerName);
					if (null != headerValues)
					{
						for (String headerValue : headerValues)
						{
							headerBuf.append(headerName).append(":")
							        .append(headerValue).append("\r\n");
							// newReq.addHeader(headerName, headerValue);
						}
					}
				}
				headerBuf.append("\r\n");
				long oldContentLen = recvReq.getContentLength();
				ChannelBuffer headerContentBuf = ChannelBuffers
				        .wrappedBuffer(headerBuf.toString().getBytes());
				long newcontentLen = oldContentLen
				        + headerContentBuf.capacity();
				newReq.setHeader("Content-Length", "" + newcontentLen);
				ChannelBuffer content = null;
				ChannelBuffer oldContentBody = recvReq.getContent();
				if (null != oldContentBody)
				{
					content = ChannelBuffers.wrappedBuffer(
					        headerContentBuf, oldContentBody);
				}
				else
				{
					content = headerContentBuf;
				}
				
				//String req = recvReq.getMethod().toString() +" " + recvReq.getUri() +" HTTP/1.0\r\n\r\n";
				if(logger.isDebugEnabled())
				{
					logger.debug("sending request" + new String(content.toByteBuffer().array()));
				}
				//content = ChannelBuffers.wrappedBuffer(req.getBytes());
				// encrpt.e
				newReq.setContent(encrypt(content));
				//newReq.setHeader("Content-Length", "" + req.length());
				event.setSource(newReq);
				break;
			}
			case RECV_HTTP_CHUNK:
			{
				HttpChunk chunk = (HttpChunk) event.getSource();
				ChannelBuffer content = encrypt(chunk.getContent());
				chunk = new DefaultHttpChunk(content);
				event.setSource(chunk);
				break;
			}
			default:
				return event;
		}
		return event;
	}

}