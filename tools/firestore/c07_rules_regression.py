import copy
import json
from pathlib import Path
import c07_rules_fixture as r

COMMIT=f'/v1/projects/{r.PROJECT}/databases/(default)/documents:commit'
RULES=Path(__file__).resolve().parents[2]/"firebase/firestore.rules"
results=[]

def expect(label,status,expected,body=None):
    ok=status==expected;results.append({'case':label,'status':status,'expected':expected,'passed':ok})
    print(('PASS ' if ok else 'FAIL ')+label+' '+str(status))
    if not ok:print(json.dumps(body));raise AssertionError(label)

def reset():
    code,result=r.api('/emulator/v1/projects/'+r.PROJECT+'/databases/(default)/documents',method='DELETE')
    assert code==200,result
    code,result=r.seed();assert code==200,result

def decode(v):
    if 'timestampValue' in v:return v
    if 'stringValue' in v:return v['stringValue']
    if 'integerValue' in v:return int(v['integerValue'])
    if 'booleanValue' in v:return v['booleanValue']
    if 'nullValue' in v:return None
    if 'arrayValue' in v:return [decode(x) for x in v['arrayValue'].get('values',[])]
    if 'mapValue' in v:return {k:decode(x) for k,x in v['mapValue'].get('fields',{}).items()}
    raise ValueError(v)

def read_job():
    code,body=r.api('/v1/'+r.PREFIX+'jobs/'+r.JOB_ID);assert code==200,body
    return {k:decode(v) for k,v in body['fields'].items()}

def transition(before,after,actor,action):
    event={'jobId':r.JOB_ID,'version':after['version'],'actorId':actor,'action':action,'before':before,'after':copy.deepcopy(after)}
    transforms=['updatedAt']+(['reviewedAt'] if action!='SUBMITTED' else [])
    return [r.write('jobs/'+r.JOB_ID,after,transforms),r.write('jobs/'+r.JOB_ID+'/history/'+str(after['version']),event,['createdAt']+['after.'+p for p in transforms])]

def main():
    assert r.BASE=='http://127.0.0.1:8088' and r.PROJECT=='demo-grandmaassist-c07'
    code,body=r.api('/emulator/v1/projects/'+r.PROJECT+':securityRules',{'rules':{'files':[{'name':'firestore.rules','content':RULES.read_text(encoding='utf-8')}]}},method='PUT');assert code==200,body
    owner=r.token()
    reset();code,body=r.api(COMMIT,{'writes':r.submission()},owner);expect('standard submission with atomic history',code,200,body)
    code,body=r.api('/v1/'+r.PREFIX+'jobs/'+r.JOB_ID,token=r.token('other-user'));expect('other account cannot read pending post',code,403,body)
    before=read_job();admin='fixture-admin'
    code,body=r.api(COMMIT,{'writes':[r.write('users/'+admin,{'role':'ADMIN','accountStatus':'ACTIVE'}),r.write('adminAccess/'+admin,{'enabled':True})]});assert code==200,body
    rejected=copy.deepcopy(before);rejected.update(version=2,status='REJECTED',reviewedBy=admin,reviewReason='')
    code,body=r.api(COMMIT,{'writes':transition(before,rejected,admin,'REJECTED')},r.token(admin));expect('admin cannot reject without reason',code,403,body)
    rejected['reviewReason']='Can bo sung DEMO'
    code,body=r.api(COMMIT,{'writes':transition(before,rejected,admin,'REJECTED')},r.token(admin));expect('admin rejects v1 to v2 with history',code,200,body)
    before=read_job();resubmitted=copy.deepcopy(before);resubmitted.update(version=3,status='PENDING_REVIEW',lastSubmissionId=r.JOB_ID+'_s3',reviewedBy='',reviewedAt=None,reviewReason='',workAddress='Dia chi DEMO moi')
    code,body=r.api(COMMIT,{'writes':transition(before,resubmitted,r.UID,'SUBMITTED')},owner);expect('owner resubmits v2 to v3 with history',code,200,body)
    before=read_job();approved=copy.deepcopy(before);approved.update(version=4,status='OPEN',reviewedBy=admin,reviewReason='')
    code,body=r.api(COMMIT,{'writes':transition(before,approved,admin,'APPROVED')},r.token(admin));expect('admin approves v3 to v4 with history',code,200,body)
    code,body=r.api('/v1/'+r.PREFIX+'jobs/'+r.JOB_ID,token=None);expect('guest can read approved post',code,200,body)
    code,body=r.api('/v1/'+r.PREFIX+'jobs/'+r.JOB_ID+'/history/1',token=None);expect('guest cannot read private history',code,403,body)

    reset();m=r.job_data();m.update(title='T'*120,description='D'*3000,workAddress='A'*500,requirementsText='R'*1000,scheduleText='S'*500,
        requiredSkillTags=[str(i)+'K'*59 for i in range(8)],workDays=['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'],
        physicalDemands=['HEAVY_LIFTING','PROLONGED_STANDING','FREQUENT_MOVEMENT'])
    code,body=r.api(COMMIT,{'writes':r.submission(m)},owner);expect('maximum content, 8 skill tags, 7 days, 3 demands',code,200,body)

    before=read_job();admin='fixture-admin'
    code,body=r.api(COMMIT,{'writes':[r.write('users/'+admin,{'role':'ADMIN','accountStatus':'ACTIVE'}),r.write('adminAccess/'+admin,{'enabled':True})]});assert code==200,body
    rejected=copy.deepcopy(before);rejected.update(version=2,status='REJECTED',reviewedBy=admin,reviewReason='Can bo sung DEMO')
    code,body=r.api(COMMIT,{'writes':transition(before,rejected,admin,'REJECTED')},r.token(admin));expect('maximum content admin rejection',code,200,body)
    before=read_job();again=copy.deepcopy(before);again.update(version=3,status='PENDING_REVIEW',lastSubmissionId=r.JOB_ID+'_s3',reviewedBy='',reviewedAt=None,reviewReason='')
    code,body=r.api(COMMIT,{'writes':transition(before,again,r.UID,'SUBMITTED')},owner);expect('maximum content resubmission',code,200,body)
    before=read_job();approved=copy.deepcopy(before);approved.update(version=4,status='OPEN',reviewedBy=admin,reviewReason='')
    code,body=r.api(COMMIT,{'writes':transition(before,approved,admin,'APPROVED')},r.token(admin));expect('maximum content admin approval',code,200,body)
    reset();unicode_job=r.job_data();unicode_job.update(title='Việc làm buổi sáng',description='Công việc nhẹ.\nCó hướng dẫn tại chỗ.',workAddress='Địa chỉ DEMO',requiredSkillTags=['Dọn dẹp'])
    code,body=r.api(COMMIT,{'writes':r.submission(unicode_job)},owner);expect('Vietnamese Unicode and multiline content',code,200,body)

    for label,mutate in [
        ('missing history',lambda w:w[:1]),
        ('owner cannot publish OPEN directly',lambda w:replace(w,'status','OPEN')),
        ('negative pay rejected',lambda w:replace(w,'payAmount',-1)),
        ('blank title rejected',lambda w:replace(w,'title','   ')),
        ('title over maximum rejected',lambda w:replace(w,'title','T'*121)),
        ('non-string optional text rejected',lambda w:replace(w,'requirementsText',5)),
        ('duplicate skill tags rejected',lambda w:replace(w,'requiredSkillTags',['Same','Same'])),
        ('non-string skill tag rejected',lambda w:replace(w,'requiredSkillTags',[1])),
        ('missing required field rejected',missing_field),
        ('history content mismatch rejected',mismatch_history),
    ]:
        reset();writes=mutate(r.submission());code,body=r.api(COMMIT,{'writes':writes},owner);expect(label,code,403,body)
    reset();code,body=r.api(COMMIT,{'writes':r.submission()},r.token('other-user'));expect('different owner cannot create post',code,403,body)
    print('Focused local Rules results:',len(results),'/',len(results),'passed')

def replace(w,key,val):
    w[0]['update']['fields'][key]=r.value(val)
    w[1]['update']['fields']['after']['mapValue']['fields'][key]=r.value(val)
    return w

def missing_field(w):
    del w[0]['update']['fields']['reviewedAt'];del w[1]['update']['fields']['after']['mapValue']['fields']['reviewedAt'];return w

def mismatch_history(w):
    w[1]['update']['fields']['after']['mapValue']['fields']['title']=r.value('Different title');return w

if __name__=='__main__':main()
