import { Hono } from 'hono';
import { cors } from 'hono/cors';
const app=new Hono();
app.use('/api/*',cors({origin:'*',allowMethods:['GET','POST','OPTIONS'],allowHeaders:['Content-Type']}));
app.get('/',c=>c.json({service:'Hishatak AI Gateway',ok:true}));
app.get('/api/ai/health',c=>c.json({ok:true,aiConfigured:!!process.env.OPENAI_API_KEY}));
app.post('/api/ai/recognize',async c=>{
 try{
  if(!process.env.OPENAI_API_KEY)return c.json({error:'AI_NOT_CONFIGURED'},503);
  const {image}=await c.req.json();
  if(typeof image!=='string'||!/^data:image\/(jpeg|png|webp);base64,/i.test(image))return c.json({error:'INVALID_IMAGE'},400);
  if(image.length>14_000_000)return c.json({error:'IMAGE_TOO_LARGE'},413);
  const prompt='Read this gravestone faithfully. Russian, Armenian and English are possible. It may contain one person or a family. Never guess unreadable letters or names; use null for uncertain structured fields. Dates must be DD.MM.YYYY only when clearly readable. Return ONLY JSON: {"language":"ru|hy|en|mixed|unknown","monumentType":"single|family|unknown","rawText":"...","people":[{"lastName":null,"firstName":null,"patronymic":null,"birthDate":null,"deathDate":null,"confidence":0,"rawText":"..."}]}';
  const body={model:'gpt-5-mini',store:false,input:[{role:'user',content:[{type:'input_text',text:prompt},{type:'input_image',image_url:image}]}],text:{format:{type:'json_object'}}};
  const r=await fetch('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+process.env.OPENAI_API_KEY,'Content-Type':'application/json'},body:JSON.stringify(body)});
  const d=await r.json();
  if(!r.ok)return c.json({error:'AI_REQUEST_FAILED',status:r.status},502);
  const out=d.output_text||d.output?.flatMap(x=>x.content||[]).find(x=>x.type==='output_text')?.text;
  if(!out)return c.json({error:'EMPTY_AI_RESPONSE'},502);
  return c.json(JSON.parse(out));
 }catch(e){return c.json({error:'RECOGNITION_FAILED'},500)}
});
export default {port:process.env.PORT||8080,fetch:app.fetch};