import base64
import copy
import datetime as dt
import json
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = 'http://127.0.0.1:8088'
PROJECT = 'demo-grandmaassist-c07'
PREFIX = f'projects/{PROJECT}/databases/(default)/documents/'
UID = 'fixture-employer'
JOB_ID = '11111111-1111-4111-8111-111111111111'
assert BASE.startswith('http://127.0.0.1:') and PROJECT.startswith('demo-')

def api(path, body=None, token='owner', method=None):
    data = None if body is None else json.dumps(body).encode()
    headers={'Content-Type': 'application/json'}
    if token is not None:headers['Authorization']='Bearer '+token
    req = urllib.request.Request(BASE + path, data=data, method=method or ('GET' if body is None else 'POST'),headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read())

def token(uid=UID, phone='+84900000000'):
    def part(v):
        return base64.urlsafe_b64encode(json.dumps(v).encode()).decode().rstrip('=')
    now = int(time.time())
    return part({'alg':'none','typ':'JWT'}) + '.' + part({
        'sub':uid, 'user_id':uid, 'iss':'https://securetoken.google.com/'+PROJECT,
        'aud':PROJECT, 'iat':now, 'exp':now+3600, 'auth_time':now,
        'phone_number':phone, 'firebase':{'sign_in_provider':'phone','identities':{'phone':[phone]}}
    }) + '.'

def stamp(days=0):
    return (dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=days)).isoformat().replace('+00:00','Z')

def timestamp(value):
    return {'timestampValue':value}

def value(v):
    if isinstance(v,dict) and list(v)==['timestampValue']:
        return v
    if v is None:return {'nullValue':None}
    if isinstance(v,bool):return {'booleanValue':v}
    if isinstance(v,int):return {'integerValue':str(v)}
    if isinstance(v,str):return {'stringValue':v}
    if isinstance(v,list):return {'arrayValue':{'values':[value(x) for x in v]}}
    if isinstance(v,dict):return {'mapValue':{'fields':fields(v)}}
    raise TypeError(type(v))

def fields(m):return {k:value(v) for k,v in m.items()}

def write(path, data, transforms=()):
    data=copy.deepcopy(data)
    for pathfield in transforms:
        target=data;parts=pathfield.split('.')
        for part in parts[:-1]:target=target[part]
        target.pop(parts[-1],None)
    w={'update':{'name':PREFIX+path,'fields':fields(data)}}
    if transforms:w['updateTransforms']=[{'fieldPath':p,'setToServerValue':'REQUEST_TIME'} for p in transforms]
    return w

def seed():
    past=timestamp(stamp(-1))
    u={'id':UID,'role':'EMPLOYER','accountStatus':'ACTIVE','displayName':'Nguoi thue DEMO',
       'phoneE164':'+84900000000','phoneVerificationMode':'FIREBASE_AUTH','phoneVerifiedAt':past,
       'version':1,'createdAt':past,'updatedAt':past,'identityStatus':'VERIFIED'}
    p={'userId':UID,'employerType':'INDIVIDUAL','organizationName':'','introduction':'Gioi thieu DEMO',
       'areaCode':'DEMO_AREA_01','createdAt':past,'updatedAt':past}
    return api(f'/v1/projects/{PROJECT}/databases/(default)/documents:commit',{
        'writes':[write('users/'+UID,u),write('employerProfiles/'+UID,p),
            write('areas/DEMO_AREA_01',{'name':'Khu vuc DEMO','active':True}),
            write('jobCategories/DEMO_WORK_01',{'name':'Cong viec DEMO','active':True})]})

def job_data():
    return {'id':JOB_ID,'employerId':UID,'employerDisplayName':'Nguoi thue DEMO',
        'title':'Vieclam DEMO','categoryId':'DEMO_WORK_01','categoryName':'Cong viec DEMO',
        'areaCode':'DEMO_AREA_01','areaName':'Khu vuc DEMO','workAddress':'Dia chi DEMO',
        'description':'Nhiem vu DEMO','requirementsText':'','requiredSkillTags':['Don dep'],
        'workDays':['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],'workShift':'MORNING',
        'scheduleText':'Lich DEMO','physicalDemands':[],'demandsDeclared':True,
        'payAmount':250000,'payUnit':'SESSION','expiresAt':timestamp(stamp(2)),
        'status':'PENDING_REVIEW','version':1,'lastSubmissionId':JOB_ID+'_s1',
        'reviewedBy':'','reviewedAt':None,'reviewReason':''}

def submission(data=None):
    m = data or job_data()
    history={'jobId':JOB_ID,'version':1,'actorId':UID,'action':'SUBMITTED','before':None,'after':dict(m)}
    return [write('jobs/'+JOB_ID,m,['createdAt','updatedAt']),
        write('jobs/'+JOB_ID+'/history/1',history,['createdAt','after.createdAt','after.updatedAt'])]

def main():
    print('LOCAL EMULATOR ONLY:',BASE,PROJECT)
    rules=(Path(__file__).resolve().parents[2]/"firebase/firestore.rules").read_text(encoding='utf-8')
    code,result=api('/emulator/v1/projects/'+PROJECT+':securityRules',{'rules':{'files':[{'name':'firestore.rules','content':rules}]}},method='PUT')
    print('load rules',code);assert code==200,result
    code,result=api('/emulator/v1/projects/'+PROJECT+'/databases/(default)/documents',method='DELETE')
    assert code==200,result
    status, result = seed()
    print('seed',status)
    assert status==200,result
    auth=token()
    for check in ['revision_20261026_3','account','phone','profile']:
        code, result=api('/v1/'+PREFIX+'jobSubmissionDiagnostics/'+UID+'/checks/'+check,token=auth)
        print('probe',check,code)
        # An allowed read of an intentionally absent document returns HTTP 404.
        assert code in (200,404),result
    code,result=api(f'/v1/projects/{PROJECT}/databases/(default)/documents:commit',{'writes':submission()},auth)
    print('submission',code,json.dumps(result))
    (Path(__file__).parent/'result.json').write_text(json.dumps({'status':code,'result':result},indent=2))
    code,coverage=api('/emulator/v1/projects/'+PROJECT+':ruleCoverage')
    if code==200:(Path(__file__).parent/'coverage.json').write_text(json.dumps(coverage))

if __name__=='__main__':main()
