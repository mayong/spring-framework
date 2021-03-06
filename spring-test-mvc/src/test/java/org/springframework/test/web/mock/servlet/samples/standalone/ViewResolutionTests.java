/*
 * Copyright 2002-2012 the original author or authors.
 *
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
 */

package org.springframework.test.web.mock.servlet.samples.standalone;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.xpath;
import static org.springframework.test.web.mock.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.mock.Person;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;
import org.springframework.web.servlet.view.xml.MarshallingView;

/**
 * Tests with view resolution.
 *
 * @author Rossen Stoyanchev
 */
public class ViewResolutionTests {

	@Test
	public void testJspOnly() throws Exception {

		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/");
		viewResolver.setSuffix(".jsp");

		standaloneSetup(new PersonController()).setViewResolvers(viewResolver).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(forwardedUrl("/WEB-INF/person/show.jsp"));
	}

	@Test
	public void testJsonOnly() throws Exception {

		standaloneSetup(new PersonController()).setSingleView(new MappingJacksonJsonView()).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(content().mimeType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.person.name").value("Corea"));
	}

	@Test
	public void testXmlOnly() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		standaloneSetup(new PersonController()).setSingleView(new MarshallingView(marshaller)).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(content().mimeType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
	}

	@Test
	public void testContentNegotiation() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		List<View> viewList = new ArrayList<View>();
		viewList.add(new MappingJacksonJsonView());
		viewList.add(new MarshallingView(marshaller));

		ContentNegotiationManager manager = new ContentNegotiationManager(
				new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

		ContentNegotiatingViewResolver cnViewResolver = new ContentNegotiatingViewResolver();
		cnViewResolver.setDefaultViews(viewList);
		cnViewResolver.setContentNegotiationManager(manager);
		cnViewResolver.afterPropertiesSet();

		MockMvc mockMvc =
			standaloneSetup(new PersonController())
				.setViewResolvers(cnViewResolver, new InternalResourceViewResolver())
				.build();

		mockMvc.perform(get("/person/Corea"))
			.andExpect(status().isOk())
			.andExpect(model().size(1))
			.andExpect(model().attributeExists("person"))
			.andExpect(forwardedUrl("person/show"));

		mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().mimeType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.person.name").value("Corea"));

		mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_XML))
			.andExpect(status().isOk())
			.andExpect(content().mimeType(MediaType.APPLICATION_XML))
			.andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
	}

	@Test
	public void defaultViewResolver() throws Exception {

		standaloneSetup(new PersonController()).build()
			.perform(get("/person/Corea"))
				.andExpect(model().attribute("person", hasProperty("name", equalTo("Corea"))))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("person/show"));  // InternalResourceViewResolver
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/person/{name}", method=RequestMethod.GET)
		public String show(@PathVariable String name, Model model) {
			Person person = new Person(name);
			model.addAttribute(person);
			return "person/show";
		}
	}

}

