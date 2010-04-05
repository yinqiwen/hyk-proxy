/**
 * This file is part of the hyk-proxy project.
 * Copyright (c) 2010 Yin QiWen <yinqiwen@gmail.com>
 *
 * Description: HttpMessageCompressPreference.java 
 *
 * @author yinqiwen [ 2010-3-28 | ����09:47:48 ]
 *
 */
package com.hyk.proxy.gae.server.util;

import java.util.List;
import java.util.regex.Pattern;

import com.hyk.compress.CompressPreference;
import com.hyk.compress.Compressor;
import com.hyk.compress.CompressorFactory;
import com.hyk.compress.CompressorType;
import com.hyk.util.thread.ThreadLocalUtil;

/**
 *
 */
public class HttpMessageCompressPreference implements CompressPreference
{
	private static Compressor compressor;
	private static int trigger;
	private static List<Pattern> ignorePatterns;
	
	public static void init(Compressor compressor, int trigger, List<Pattern> ignorePatterns)
	{
		HttpMessageCompressPreference.compressor = compressor;
		HttpMessageCompressPreference.trigger = trigger;
		HttpMessageCompressPreference.ignorePatterns = ignorePatterns;
	}
	
	@Override
	public Compressor getCompressor()
	{
		String contentType = ThreadLocalUtil.getThreadLocalUtil(String.class).getThreadLocalObject();
		if(null != contentType)
		{
			if(null != ignorePatterns)
			{
				for(Pattern p:ignorePatterns)
				{
					if(p.matcher(contentType.toLowerCase()).matches())
					{
						return CompressorFactory.getCompressor(CompressorType.NONE);
					}
				}
			}
		}
		return compressor;
	}

	@Override
	public int getTrigger()
	{
		return trigger;
	}

}
