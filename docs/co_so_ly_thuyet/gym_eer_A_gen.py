# EER diagram generator (Elmasri-Navathe notation), design A (with VISITOR).
# Run:  python3 gym_eer_A_gen.py && neato -n2 -Tpng -Gdpi=150 gym_eer_A.dot -o gym_eer_A.png
# Online viewers (GraphvizOnline): python3 gym_eer_A_gen.py portable  -> paste gym_eer_A_portable.dot, choose engine "neato" (NOT dot)
import math
N=[]; E=[]
FONT="Helvetica"
def esc(s): return s
import sys
PORTABLE = len(sys.argv)>1 and sys.argv[1]=="portable"   # portable: positions pinned in inches, works with plain "neato" and online viewers
def pt(x,y): return f'pos="{x/72:.3f},{y/72:.3f}!"' if PORTABLE else f'pos="{x:.0f},{y:.0f}"'
ents={}   # name -> (x,y,kind)
S=0.62
def entity(name,x,y,weak=False):
    x,y=x*S,y*S
    ents[name]=(x,y)
    per=2 if weak else 1
    N.append(f'"{name}" [shape=box, peripheries={per}, style=filled, fillcolor="#DCEBFA", fontsize=23, fontname="{FONT}-Bold", margin="0.28,0.16", {pt(x,y)}];')
RELS=set()
def rel(name,x,y,ident=False,label=None):
    x,y=x*S,y*S
    ents[name]=(x,y); RELS.add(name)
    per=2 if ident else 1
    lab=label or name.replace("_"," ")
    N.append(f'"{name}" [label="{lab}", shape=diamond, peripheries={per}, style=filled, fillcolor="#FCE6B0", fontsize=17, fontname="{FONT}-Bold", margin="0.12,0.06", {pt(x,y)}];')
def circ(name,x,y,txt="d"):
    x,y=x*S,y*S
    ents[name]=(x,y)
    N.append(f'"{name}" [label="{txt}", shape=circle, style=filled, fillcolor="#E4E4DC", fontsize=12, fixedsize=true, width=0.42, {pt(x,y)}];')
def attrs(owner,items,arcs,rx=135,ry=80,partial_key=None,sep=0):
    """items: list of (label, flags) flags: 'k' key, 'p' partial key. arcs: list of (a1,a2) degrees (0=right, 90=up)."""
    ox,oy=ents[owner]
    n=len(items)
    # distribute n items over arcs proportional to arc length
    tot=sum(abs(b-a) for a,b in arcs)
    angs=[]
    # compute per-arc counts
    counts=[0]*len(arcs)
    rem=n
    for i,(a,b) in enumerate(arcs):
        c=round(n*abs(b-a)/tot) if i<len(arcs)-1 else rem
        c=min(c,rem); counts[i]=c; rem-=c
    for (a,b),c in zip(arcs,counts):
        for j in range(c):
            t=(j+0.5)/c
            angs.append(a+(b-a)*t)
    for (lab,fl),ang in zip(items,angs):
        f=1.0
        if n>=4 and angs.index(ang)%2==1: f=1.45
        hw=6.4*len(owner)+34 if owner not in RELS else 60
        ow=4.3*len(lab)+34
        big=1.3 if n>=6 else 1.0
        RX=big*(hw+ow*0.62+28); RY=big*((34 if owner not in RELS else 28)+30+22)
        x=ox+f*RX*math.cos(math.radians(ang)); y=oy+f*RY*math.sin(math.radians(ang))
        nm=f"{owner}.{lab}"
        if 'k' in fl:  text=f'<<U>{lab}</U>>'
        elif 'p' in fl: text=f'<<TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="1"><TR><TD SIDES="B" STYLE="dashed" BORDER="1">{lab}</TD></TR></TABLE>>'
        else: text=f'"{lab}"'
        N.append(f'"{nm}" [label={text}, shape=ellipse, style=filled, fillcolor="white", fontsize=17, fontname="{FONT}", margin="0.1,0.05", {pt(x,y)}];')
        E.append(f'"{nm}" -- "{owner}" [color="#555555", penwidth=0.8];')
def attr_at(owner,lab,dx,dy,key=False):
    ox,oy=ents[owner]; x,y=ox+dx,oy+dy; nm=f"{owner}.{lab}"
    N.append(f'"{nm}" [label={('<<U>'+lab+'</U>>') if key else chr(34)+lab+chr(34)}, shape=ellipse, style=filled, fillcolor="white", fontsize=17, fontname="{FONT}", margin="0.1,0.05", {pt(x,y)}];')
    E.append(f'"{nm}" -- "{owner}" [color="#555555", penwidth=0.8];')
CARD_N=[0]
def link(a,b,card=None,total=False,headlab=None,ang=-42):
    col='"black:white:black"' if total else '"black"'
    pw=1.8 if total else 1.2
    E.append(f'"{a}" -- "{b}" [color={col}, penwidth={pw}];')
    if card:
        ent,oth=(b,a) if (a in RELS) else (a,b)       # label sits next to the ENTITY end of the line
        ex,ey=ents[ent]; ox,oy=ents[oth]
        dx,dy=ox-ex,oy-ey; L=math.hypot(dx,dy); ux,uy=dx/L,dy/L
        hw=0.5*(len(ent)*19+40)+6; hh=30
        t=min(hw/abs(ux) if abs(ux)>1e-6 else 1e9, hh/abs(uy) if abs(uy)>1e-6 else 1e9)
        d=t+28
        px,py=-uy,ux
        x=ex+ux*d+px*17; y=ey+uy*d+py*17
        CARD_N[0]+=1
        N.append(f'"card{CARD_N[0]}" [label="{card}", shape=plaintext, fontsize=28, fontname="{FONT}-Bold", width=0.2, height=0.2, margin="0.0,0.0", {pt(x,y)}];')
def spec(sup,circle,subs,total=False):
    link(sup,circle,total=total)
    for s in subs:
        E.append(f'"{circle}" -- "{s}" [color="black", penwidth=1.0, label="  ⊂", fontsize=20, fontname="{FONT}-Bold"];')

# ---------------- Membership & Billing (lower left) ----------------
entity("MEMBERSHIP_PLAN",600,520)
rel("CLASSIFIED_UNDER",600,200,label="CLASSIFIED\\nUNDER")
entity("MEMBERSHIP",600,-150)
rel("HOLDS",1250,-150)
entity("MEMBER",1900,-150)
circ("VISITOR_d",1900,-390)
entity("VISITOR",1900,-620)
entity("DAILY_PASS_TICKET",2700,-390)
rel("GENERATES",600,-390)
entity("BILLING_LOG",600,-620)
rel("BILLED_TO",1250,-620,label="BILLED TO")

# ---------------- Schedule (centre) ----------------
entity("STAFF",1500,1700)
circ("STAFF_d",1500,1480)
entity("TRAINER",1500,1260)
entity("RECEPTIONIST",900,1350)
rel("LEADS",1700,1010)
entity("ROOM",2400,1260)
rel("HOSTS",2150,1010)
entity("SESSION",1900,780)
circ("SESSION_d",1900,520)
entity("CLASS",1300,250)
entity("PT_BOOKING",2500,250)
rel("REGISTER",1600,50)
rel("BOOKS_PT",2200,50,label="BOOKS PT")

# ---------------- Facility (right) ----------------
rel("CONTAINS",2850,1260)
entity("EQUIPMENT",3350,1260)
rel("USES",3350,800)
rel("LOGGED_FOR",3350,1540,ident=True,label="LOGGED\\nFOR")
entity("MAINTENANCE_LOG",3350,1830,weak=True)
rel("SIGNS",2450,1830)

# links
link("MEMBERSHIP_PLAN","CLASSIFIED_UNDER","1"); link("CLASSIFIED_UNDER","MEMBERSHIP","N",total=True)
link("MEMBER","HOLDS","1"); link("HOLDS","MEMBERSHIP","N",total=True)
link("MEMBERSHIP","GENERATES","1"); link("GENERATES","BILLING_LOG","N")
link("VISITOR","BILLED_TO","1"); link("BILLED_TO","BILLING_LOG","N",total=True)
spec("VISITOR","VISITOR_d",["MEMBER","DAILY_PASS_TICKET"],total=True)
spec("STAFF","STAFF_d",["TRAINER","RECEPTIONIST"],total=False)
link("TRAINER","LEADS","1"); link("LEADS","SESSION","N",total=True)
link("ROOM","HOSTS","1"); link("HOSTS","SESSION","N",total=True)
spec("SESSION","SESSION_d",["PT_BOOKING","CLASS"],total=True)
link("MEMBER","BOOKS_PT","1",ang=40); link("BOOKS_PT","PT_BOOKING","N",total=True,ang=40)
link("MEMBER","REGISTER","M"); link("REGISTER","CLASS","N")
link("ROOM","CONTAINS","1"); link("CONTAINS","EQUIPMENT","N")
link("USES","SESSION","M"); link("USES","EQUIPMENT","N")
link("EQUIPMENT","LOGGED_FOR","1"); link("LOGGED_FOR","MAINTENANCE_LOG","N",total=True)
link("STAFF","SIGNS","1"); link("SIGNS","MAINTENANCE_LOG","N",total=True)

# ---------------- attributes ----------------
attrs("MEMBERSHIP_PLAN",[("Plan_ID","k"),("Plan_Name",""),("Duration_Months",""),("Price",""),("Description","")],[(20,160)],rx=190,ry=120)
attrs("MEMBERSHIP",[("Membership_ID","k"),("Start_Date",""),("End_Date",""),("Status","")],[(140,250)],rx=170,ry=100)
attrs("VISITOR",[("Visitor_ID","k"),("First_Name",""),("Last_Name",""),("Phone","")],[(200,340)],rx=190,ry=110)
attrs("MEMBER",[("Email",""),("Birth_Date",""),("Photo",""),("Registration_Date","")],[(215,255),(-35,15)],rx=290,ry=170)
attrs("BILLING_LOG",[("Bill_ID","k"),("Amount_Paid",""),("Billing_Status",""),("Invoice_Date",""),("Due_Date",""),("Payment_Date","")],[(170,330)],rx=200,ry=110)
attrs("DAILY_PASS_TICKET",[("Visit_Date",""),("Daily_Rate","")],[(20,160)],rx=190,ry=110)
attrs("STAFF",[("Staff_ID","k"),("Full_Name",""),("Phone",""),("Role",""),("Hire_Date",""),("Salary","")],[(125,250)],rx=190,ry=120)
attrs("TRAINER",[("Specialty",""),("Certification","")],[(170,230)],rx=140,ry=90)
attrs("RECEPTIONIST",[("Shift_Type","")],[(100,100)],rx=0,ry=90)
attrs("ROOM",[("Room_ID","k"),("Room_Name",""),("Capacity",""),("Floor_Location","")],[(45,165)],rx=170,ry=100)
attrs("SESSION",[("Session_ID","k"),("Session_Type","")],[(190,240)],rx=190,ry=100); attr_at("SESSION","Start_Time",140,-115); attr_at("SESSION","End_Time",275,-85)
attrs("PT_BOOKING",[("Booking_Status","")],[(-60,40)],rx=190,ry=60)
attrs("CLASS",[("Class_Name",""),("Max_Capacity","")],[(140,230)],rx=190,ry=100)
attr_at("EQUIPMENT","Equipment_ID",200,85,key=True); attr_at("EQUIPMENT","Equipment_Name",205,25); attr_at("EQUIPMENT","Operational_Status",225,-35); attr_at("EQUIPMENT","Purchase_Date",200,-95)
attr_at("USES","Reserved_From",-95,-70); attr_at("USES","Reserved_To",95,-70)
attrs("MAINTENANCE_LOG",[("Log_no","p"),("Maintenance_Date",""),("Cost",""),("Description","")],[(20,160)],rx=240,ry=120)
attr_at("REGISTER","Attendance_Status",115,75); attr_at("REGISTER","Registration_Date",-150,-55)

out='graph EER {\n  graph [splines=line, outputorder=edgesfirst, bgcolor="white", pad=0.3];\n  node [fontname="Helvetica"];\n  edge [fontname="Helvetica"];\n'+'\n'.join(N)+'\n'+'\n'.join(E)+'\n}\n'
import os
name='gym_eer_A_portable.dot' if PORTABLE else 'gym_eer_A.dot'
path=os.path.join(os.path.dirname(os.path.abspath(__file__)),name)
try:
    open(path,'w',encoding='utf-8').write(out)
except PermissionError:
    path=os.path.join(os.path.dirname(os.path.abspath(__file__)),'new_'+name)   # old file is locked or read-only: write a new one
    open(path,'w',encoding='utf-8').write(out)
print("Wrote",path)
