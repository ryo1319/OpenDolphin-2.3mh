package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.StampModel;
import open.dolphin.session.StampServiceBean;

/**
 * StampResource
 *
 * @author kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("stamp")
public class StampResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private StampServiceBean stampServiceBean;

    public StampResource() {
    }

    @GET
    @Path("id/{param}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getStamp(@PathParam("param") String param) {
        
        StampModel stamp = stampServiceBean.getStamp(param);
        
        StreamingOutput so = getJsonOutStream(stamp);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getStamps(@QueryParam(IDS) String ids) {

        List<String> list = getConverter().toStrList(ids);

        List<StampModel> result = stampServiceBean.getStamp(list);
        
        TypeReference typeRef = new TypeReference<List<StampModel>>(){};
        StreamingOutput so = getJsonOutStream(result, typeRef);
        
        return Response.ok(so).build();
    }

    @PUT
    @Path("id")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putStamp(String json) {

        StampModel model = getConverter().fromJson(json, StampModel.class);

        String ret = stampServiceBean.putStamp(model);
        //debug(ret);
        
        return Response.ok(ret).build();
    }

    @PUT
    @Path("list")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putStamps(String json) {
        
        TypeReference<List<StampModel>> typeRef = 
                new TypeReference<List<StampModel>>(){};
        List<StampModel> list = getConverter().fromJson(json, typeRef);

        List<String> ret = stampServiceBean.putStamp(list);
        
        String retText = getConverter().fromList(ret);
        
        return Response.ok(retText).build();
    }


    @DELETE
    @Path("id/{param}")
    public void deleteStamp(@PathParam("param") String param) {

        int cnt = stampServiceBean.removeStamp(param);

        debug(String.valueOf(cnt));
    }
    

    @DELETE
    @Path("list")
    public void deleteStamps(@QueryParam(IDS) String ids) {

        List<String> list = getConverter().toStrList(ids);

        int cnt = stampServiceBean.removeStamp(list);

        debug(String.valueOf(cnt));
    }
    
    
    @POST
    @Path("postRemoveStamps")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postRemoveStamps(String json) {
        
        TypeReference<List<String>> typeRef = 
                new TypeReference<List<String>>(){};
        List<String> list = getConverter().fromJson(json, typeRef);
        
        int cnt = stampServiceBean.removeStamp(list);
        String ret = String.valueOf(cnt);
        
        debug(ret);
        
        return Response.ok(ret).build();
    }
    
    @GET
    @Path("allStamps/{param}")
    public Response getAllStamps(@PathParam("param") Long userId) {
        
        List<StampModel> stamps = stampServiceBean.getAllStamps(userId);
        
        TypeReference typeRef = new TypeReference<List<StampModel>>(){};
        StreamingOutput so = getGzipOutStream(stamps, typeRef);
        
        return Response.ok(so).build();
    }

    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
