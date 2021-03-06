package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.RegexBody.regex;

public class AssemblyTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    public Assembly assembly;

    @Before
    public void setUp() throws Exception {
        assembly = new Assembly(transloadit);
    }

    @Test
    public void addFile() throws Exception {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.files.get("file_name"));
    }

    @Test
    public void addInputStreamFile() throws Exception {
        InputStream file = new FileInputStream(new File("LICENSE"));
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.fileStreams.get("file_name"));
    }

    @Test
    public void addCombinedFiles() throws Exception {
        InputStream fileStream = new FileInputStream(new File("LICENSE"));
        assembly.addFile(fileStream, "file_name");

        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertFalse(assembly.fileStreams.containsKey("file_name"));
        assertEquals(file, assembly.files.get("file_name"));
    }


    @Test
    public void removeFile() throws Exception {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertTrue(assembly.files.containsKey("file_name"));

        assembly.removeFile("file_name");
        assertFalse(assembly.files.containsKey("file_name"));
    }

    @Test
    public void save() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        assembly.addFile(new File("LICENSE"), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");

        mockServerClient.reset();
    }

    @Test
    public void saveWithInputStream() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        assembly.addFile(new FileInputStream(new File("LICENSE")), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");

        mockServerClient.reset();
    }

    @Test
    public void saveWithTus() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1" +
                        "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
    }

    @Test
    public void saveWithInputStreamAndTus() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new FileInputStream(new File("LICENSE")), "file_name");

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1" +
                        "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
    }
}
