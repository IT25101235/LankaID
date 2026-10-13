from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
out=Path(__file__).parent
S=3
font=lambda n,b=False:ImageFont.truetype(r'C:\Windows\Fonts\arialbd.ttf' if b else r'C:\Windows\Fonts\arial.ttf',n*S)
def text(d,x,y,t,n=16,b=False,center=False):
 d.text((x*S,y*S),t,font=font(n,b),fill='black',anchor='mm' if center else None)
def connect(d,points,a='1',b='0..N'):
 d.line([(x*S,y*S) for x,y in points],fill='#555555',width=2*S)
 for k,((x,y),label) in enumerate([(points[0],a),(points[-1],b)]):
  if k==0 and points[0]==(613,418):continue
  nx,ny=points[1] if k==0 else points[-2]
  dx,dy=nx-x,ny-y
  if dx:
   bx=x+4 if dx>0 else x-34;by=y-19 if k==0 else y+5
  else:
   bx=x+5;by=y+5 if dy>0 else y-19
  d.rectangle((bx*S,by*S,(bx+31)*S,(by+16)*S),fill='white');text(d,bx,by,label,12)
def box(d,x,y,title,fields,w=265,h=164,ghost=False):
 d.rectangle((x*S,y*S,(x+w)*S,(y+h)*S),fill='#F5F5F5' if ghost else 'white',outline='black',width=2*S)
 d.rectangle((x*S,y*S,(x+w)*S,(y+32)*S),fill='#E2E2E2',outline='black',width=2*S)
 text(d,x+w/2,y+16,title,17 if len(title)<24 else 15,True,True)
 for i,f in enumerate(fields):text(d,x+9,y+43+i*23,f,15)
# Selected keys and relationships, checked against SQL and current JPA mappings.
im=Image.new('RGB',(960*S,970*S),'white');d=ImageDraw.Draw(im)
xs=[35,348,660];ys=[30,260,490,720]
# Edges are drawn first, with entity boxes masking routes outside their ports.
connect(d,[(613,110),(660,110)],'1','0..1')
connect(d,[(348,135),(323,135),(323,300),(300,300)],'1','0..1')
connect(d,[(166,194),(166,260)],'0..1','0..N')
connect(d,[(300,344),(348,344)],'1','0..N')
connect(d,[(613,340),(660,340)],'1','0..N')
connect(d,[(348,389),(322,389),(322,550),(300,550)],'1','0..1')
connect(d,[(348,408),(307,408),(307,784),(300,784)],'1','0..1')
connect(d,[(481,424),(481,490)],'1','0..1')
connect(d,[(613,390),(637,390),(637,550),(660,550)],'1','0..N')
connect(d,[(613,418),(624,418),(624,680),(481,680),(481,720)],'1','0..N')
connect(d,[(613,406),(650,406),(650,780),(660,780)],'1','0..N')
items=[
(0,0,'GovernmentIdentityRecord',['PK government_record_id','nic_no / passport_no','registered_email','registered_phone','identity_status']),
(1,0,'UserAccount',['PK user_id','UQ email / phone','password_hash','account_status','email_verified']),
(2,0,'StaffAccount',['PK FK user_id','UQ employee_no','staff_role / staff_status','FK created_by_staff_id']),
(0,1,'CitizenAccount',['PK FK user_id','FK government_record_id','family_name / given_name','date_of_birth','address fields']),
(1,1,'Application',['PK application_id','FK citizen_id','UQ application_no','service_type / kind','current_status']),
(2,1,'ApplicationDocument',['PK document_id','FK application_id','FK verified_by_staff_id','document_type','file_path']),
(0,2,'NICApplication',['PK FK application_id','last_nic_no','last_issued_date','police_station','police_report_date']),
(1,2,'DocumentDelivery',['PK delivery_id','FK UQ application_id','FK centre_id','delivery_method / status','tracking_reference']),
(2,2,'ApplicationReview',['PK review_id','FK application_id','FK staff_id','review_sequence','decision / reviewed_at']),
(0,3,'PassportApplication',['PK FK application_id','service_level','travel_document_type','nic_proof_no','parent_nic_no']),
(1,3,'StatusUpdate',['PK status_update_id','FK application_id','FK changed_by_staff_id','status / changed_at','note']),
(2,3,'DirectorReviewReferral',['PK referral_id','FK application_id','FK referred_by_staff_id','FK director_id','FK withdrawn_by_staff_id'])]
for c,r,t,f in items:box(d,xs[c],ys[r],t,f)
text(d,480,924,'PK primary key   FK foreign key   UQ unique constraint',15,False,True)
text(d,480,950,'1 one   0..1 optional single record   0..N zero or many records',15,False,True)
im.save(out/'er-accounts.png')
im=Image.new('RGB',(960*S,1200*S),'white');d=ImageDraw.Draw(im)
ys=[25,250,475,700,925]
connect(d,[(300,103),(348,103)],'0..N','1')
connect(d,[(613,103),(660,103)],'1','0..N')
connect(d,[(481,189),(481,250)],'1','0..N')
connect(d,[(760,189),(760,225),(613,225),(613,312)],'0..1','0..N')
connect(d,[(348,356),(321,356),(321,529),(300,529)],'1','0..1')
connect(d,[(481,414),(481,475)],'1','0..N')
connect(d,[(613,374),(640,374),(640,529),(660,529)],'1','0..1')
connect(d,[(795,700),(795,675),(330,675),(330,420),(166,420),(166,414)],'1','0..N')
items=[
(0,0,'WalkInSlot',['PK slot_id','FK centre_id','slot_date / slot_time','slot_capacity','is_available']),
(1,0,'Centre',['PK centre_id','centre_name','city / district','daily_capacity']),
(2,0,'BiometricAppointmentSlot',['PK slot_id','FK centre_id','service_type','slot_date / slot_time','slot_capacity / is_available']),
(0,1,'UserNotification',['PK FK notification_id','PK FK user_id','delivered_at','read_at']),
(1,1,'Appointment',['PK appointment_id','FK citizen_id / application_id','FK centre_id / biometric_slot_id','UQ reference_no','status / scheduled_at / version']),
(2,1,'Links to Figure 3',['CitizenAccount / Application','StaffAccount / UserAccount','StatusUpdate','FK fields identify these links']),
(0,2,'Payment',['PK payment_id','FK UQ appointment_id','UQ transaction_reference','amount / paid_at','payment_status']),
(1,2,'AppointmentChangePayment',['PK change_payment_id','FK appointment_id','request_token / change_type','amount','previous_scheduled_at']),
(2,2,'AppointmentCheckIn',['PK FK appointment_id','FK staff_id','checked_in_at','check_in_method']),
(0,3,'SupportRequest',['PK request_id','FK citizen_id','FK handled_by_staff_id','request_type / subject','message']),
(1,3,'BiometricCollection',['PK FK application_id','FK staff_id','collected_at','collection_status']),
(2,3,'Notification',['PK notification_id','FK sent_by_staff_id','FK generated_by_status_update_id','notification_type / message','created_at / is_broadcast']),
(0,4,'ApplicationFeeSettings',['PK settings_id','nic_fee / passport_fee','updated_at']),
(1,4,'PublicAnnouncement',['PK announcement_id','title / message','announcement_type','starts_at / expires_at']),
(2,4,'SystemMaintenance',['PK maintenance_id','maintenance_active','maintenance_message','updated_at'])]
for c,r,t,f in items:box(d,xs[c],ys[r],t,f,ghost=t=='Links to Figure 3')
text(d,480,1130,'PK primary key   FK foreign key   UQ unique constraint',15,False,True)
text(d,480,1156,'Selected relationships drawn; FK fields retain cross-figure ownership links',15,False,True)
text(d,480,1182,'Each application has at most one BiometricCollection; each check-in uses its appointment key',14,False,True)
im.save(out/'er-operations.png')
