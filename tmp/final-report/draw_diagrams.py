from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
out=Path(__file__).parent
scale=3
font=lambda size:ImageFont.truetype(r'C:\Windows\Fonts\arial.ttf',size*scale)
def txt(d,xy,s,size=13,anchor='mm'):
 d.multiline_text(tuple(int(v*scale) for v in xy),s,font=font(size),fill='black',anchor=anchor,align='center',spacing=3*scale)
def line(d,points):d.line([tuple(int(v*scale) for v in p) for p in points],fill='black',width=2*scale)
def actor(d,x,y,label):
 d.ellipse(((x-9)*scale,(y-28)*scale,(x+9)*scale,(y-10)*scale),outline='black',width=2*scale)
 line(d,[(x,y-10),(x,y+20)]);line(d,[(x-17,y),(x+17,y)]);line(d,[(x,y+20),(x-15,y+39)]);line(d,[(x,y+20),(x+15,y+39)])
 txt(d,(x,y+62),label,11)
def oval(d,x,y,label,w=166,h=60):
 d.ellipse(((x-w/2)*scale,(y-h/2)*scale,(x+w/2)*scale,(y+h/2)*scale),fill='white',outline='black',width=2*scale)
 txt(d,(x,y),label,12)
im=Image.new('RGB',(690*scale,760*scale),'white');d=ImageDraw.Draw(im)
d.rectangle((120*scale,15*scale,575*scale,718*scale),outline='black',width=2*scale);txt(d,(347,38),'LankaID final prototype',15)
actor(d,55,338,'Citizen');actor(d,638,338,'NIC or passport\nofficer')
left=['Register and\nauthenticate','Create application\nand upload evidence','Renew or replace\nexisting document','Track status and\ncorrect application','Pay fee and choose\ndelivery or collection','Book or change\nbiometric appointment','Get QR pass and\nraise support request']
right=['Review assigned\napplications','Approve or reject\nwith reason','Request correction\nor refer to director','Publish centre\nappointment slots','Check in by QR\nor reference','Record biometric\ncompletion']
for i,s in enumerate(left):
 y=100+i*87;line(d,[(55,345),(135,y)]);oval(d,218,y,s)
for i,s in enumerate(right):
 y=135+i*100;line(d,[(638,345),(559,y)]);oval(d,475,y,s)
d.rectangle((8*scale,385*scale,105*scale,423*scale),fill='white');txt(d,(55,400),'Citizen',11)
d.rectangle((584*scale,384*scale,689*scale,432*scale),fill='white');txt(d,(638,400),'NIC or passport\nofficer',11)
txt(d,(345,744),'Protected use cases require an authenticated session',11)
im.save(out/'usecases-citizen-officer.png')
im=Image.new('RGB',(690*scale,800*scale),'white');d=ImageDraw.Draw(im)
d.rectangle((125*scale,15*scale,680*scale,770*scale),outline='black',width=2*scale);txt(d,(405,38),'LankaID final prototype',15)
groups=[('Public visitor',['Verify mobile and\nbook walk-in','Pay for visit and\nrecover QR pass','Read public guidance\nand use assistant']),('CSR',['Manage support\nrequests and replies','Manage walk-in\ncheck-in and service','Record collection\nand handover']),('Director',['View performance\nand operational reports','Resolve referred\nreview cases','Process printing\nand export register']),('Administrator',['Manage citizens,\nstaff and centres','Set fees and\npublish announcements','Control\nmaintenance mode'])]
for i,(a,uses) in enumerate(groups):
 y=145+i*185;actor(d,57,y-12,a)
 cy=y-5
 line(d,[(57,cy),(144,cy)])
 line(d,[(57,cy),(110,cy-65),(405,cy-65),(405,cy-41)])
 line(d,[(57,cy),(110,cy+63),(585,cy+63),(585,cy+41)])
 for j,s in enumerate(uses):
  x=225+j*180;oval(d,x,cy,s,w=162,h=82)
 if i<3:line(d,[(137,y+81),(668,y+81)])
txt(d,(405,745),'CSR functions are also available to the director',11)
im.save(out/'usecases-operations.png')
