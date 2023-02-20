package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.XpathService;

import java.time.Instant;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.HtmlProcessorService;


import java.util.Optional;
import java.util.UUID;

import java.io.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

@Service
@Slf4j
public class ImageBamHost extends Host {

  private static final String host = "imagebam.com";
  private static final String IMG_XPATH = "//img[contains(@class,'main-image')]";
  private static final String CONTINUE_XPATH = "//*[contains(text(), 'Continue')]";
  
  private final ConnectionService cm;
  private final HostService hostService;
  private final XpathService xpathService;
  private final HtmlProcessorService htmlProcessorService;

  @Autowired
  public ImageBamHost(
      ConnectionService cm,
      HostService hostService,
      XpathService xpathService,
      HtmlProcessorService htmlProcessorService) {
    this.cm = cm;
    this.hostService = hostService;
    this.xpathService = xpathService;
    this.htmlProcessorService = htmlProcessorService;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getLookup() {
    return host;
  }

  @Override
  public HostService.NameUrl getNameAndUrl(final String url, final HttpClientContext context)
      throws HostException {

    Document doc = hostService.getResponse(url, context).getDocument();

	  
	HttpClient client = cm.getClient().build();
	HttpGet httpGet = cm.buildHttpGet(url, context);
	httpGet.addHeader("Referer", url);
	long expireTime=Instant.now().getEpochSecond();
	expireTime+=6*60*60*1000;
	httpGet.addHeader("Cookie", "nsfw_inter=1");
	HttpResponse httpResponse;
	
	try {
		httpResponse = client.execute(httpGet);
	} catch (Exception e) {
		throw new HostException(e);
	}
	  
	try {
		doc = htmlProcessorService.clean(EntityUtils.toString(httpResponse.getEntity()));
	} catch (Exception e) {
		throw new HostException(e);
	}

    Node imgNode;
    try {
      log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
      imgNode = xpathService.getAsNode(doc, IMG_XPATH);
    } catch (XpathException e) {
      throw new HostException(e);
    }

    if (imgNode == null) {
      throw new HostException(String.format("Xpath '%s' cannot be found in '%s'", IMG_XPATH, url));
    }

    try {
      log.debug(String.format("Resolving name and image url for %s", url));
      String imgTitle =
          Optional.ofNullable(imgNode.getAttributes().getNamedItem("alt"))
              .map(e -> e.getTextContent().trim())
              .orElse("");
      String imgUrl =
          Optional.ofNullable(imgNode.getAttributes().getNamedItem("src"))
              .map(e -> e.getTextContent().trim())
              .orElse("");
      String defaultName = UUID.randomUUID().toString();

      int index = imgUrl.lastIndexOf('/');
      if (index != -1 && index < imgUrl.length()) {
        defaultName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
      }

      return new HostService.NameUrl(imgTitle.isEmpty() ? defaultName : imgTitle, imgUrl);
    } catch (Exception e) {
      throw new HostException("Unexpected error occurred", e);
    }
  }
}