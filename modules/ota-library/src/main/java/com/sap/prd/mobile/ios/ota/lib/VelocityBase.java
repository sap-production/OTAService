/*
 * #%L
 * Over-the-air deployment library
 * %%
 * Copyright (C) 2012 SAP AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.sap.prd.mobile.ios.ota.lib;

import static org.apache.velocity.util.StringUtils.nullTrim;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;

import com.sap.prd.mobile.ios.ota.lib.VelocityBase.IParameters;

/**
 * Base class for the Velocity based Generators Each generator has to provide an IParameters
 * implementation to provide the parameters used in the template.
 * 
 * @param <P>
 */
public abstract class VelocityBase<P extends IParameters>
{

  protected final Template template;
  protected final String templateName;

  protected VelocityBase(String templateName)
  {
    if (nullTrim(templateName) == null) throw new IllegalArgumentException("templateName not specified");
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "class,jar,file");
    ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    ve.setProperty("jar.resource.loader.class", "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
    File templateFile = new File(templateName);
    if (templateFile.isFile()) {
      ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateFile.getParent());
      template = ve.getTemplate(templateFile.getName());
      this.templateName = template==null ? null : templateFile.getAbsolutePath();
    }
    else {
      template = ve.getTemplate(templateName);
      this.templateName = template==null ? null : templateName;
    }
    if(template == null) throw new ResourceNotFoundException("Neither file nor resource found for '"+templateName+"'");
  }

  public String generate(P parameters) throws IOException
  {
    StringWriter swriter = new StringWriter();
    PrintWriter writer = new PrintWriter(swriter);
    generate(writer, parameters);
    writer.flush();
    writer.close();
    return swriter.getBuffer().toString();
  }

  public synchronized void generate(PrintWriter writer, P parameters) throws IOException
  {
    VelocityContext context = new VelocityContext();
    Map<String, Object> mappings = parameters.getMappings();
    for (String key : mappings.keySet()) {
      context.put(key, mappings.get(key));
    }
    template.merge(context, writer);
  }

  static interface IParameters
  {
    /**
     * key/value pairs to be put into the <code>VelocityContext</code>.
     * 
     * @return
     */
    public Map<String, Object> getMappings();
  }

  static class Parameters implements IParameters
  {
    protected final Map<String, Object> mappings;

    protected Parameters()
    {
      this.mappings = new HashMap<String, Object>();
    }

    @Override
    public Map<String, Object> getMappings()
    {
      return mappings;
    }

  }
  
  public String getTemplateName()
  {
    return templateName;
  }

}
