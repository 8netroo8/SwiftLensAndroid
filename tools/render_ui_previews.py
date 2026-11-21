from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / "ui-previews"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 540, 1200
BLUE = "#2878F0"
NAVY = "#16243A"
PAGE = "#F5F7FA"
MUTED = "#768397"
PALE = "#EAF2FF"
WHITE = "#FFFFFF"

def font(size, bold=False):
    choices = (["C:/Windows/Fonts/msyhbd.ttc", "C:/Windows/Fonts/segoeuib.ttf", "C:/Windows/Fonts/arialbd.ttf"] if bold else ["C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/segoeui.ttf", "C:/Windows/Fonts/arial.ttf"])
    choices += ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]
    for path in choices:
        if Path(path).exists():
            try: return ImageFont.truetype(path, size)
            except OSError: pass
    return ImageFont.load_default()

def rr(d, box, fill, radius=20, outline=None, width=1):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)

def text(d, x, y, value, size=18, color=NAVY, bold=False):
    d.text((x, y), value, font=font(size, bold), fill=color)

def wrap(d, x, y, value, width, size=15, color=MUTED, bold=False, gap=7):
    f=font(size,bold); line=""; yy=y
    for char in value:
        trial=line+char
        if d.textbbox((0,0),trial,font=f)[2] > width and line:
            if " " in line and char != " ":
                head, tail=line.rsplit(" ",1)
                d.text((x,yy),head,font=f,fill=color); yy += size+gap; line=tail+char
            else:
                d.text((x,yy),line.rstrip(),font=f,fill=color); yy += size+gap; line=char.lstrip()
        else: line=trial
    if line: d.text((x,yy),line,font=f,fill=color); yy += size+gap
    return yy

def base(page, title, subtitle=None, root=False):
    im=Image.new("RGB",(W,H),PAGE); d=ImageDraw.Draw(im)
    text(d,24,12,"9:41",15,NAVY,True)
    d.text((W-108,12),"LTE  WiFi  100%",font=font(12,True),fill=NAVY)
    if root:
        text(d,24,60,title,30,NAVY,True)
        if subtitle: text(d,24,101,subtitle,15,MUTED)
    else:
        text(d,24,55,"‹",35,BLUE,True); text(d,78,64,title,25,NAVY,True)
        if subtitle: text(d,24,108,subtitle,14,MUTED)
    return im,d

def card(d, y, h, fill=WHITE, x=20, w=500, radius=20): rr(d,(x,y,x+w,y+h),fill,radius)

def button(d,y,label,x=24,w=492,fill=BLUE,fg=WHITE):
    rr(d,(x,y,x+w,y+54),fill,15); bb=d.textbbox((0,0),label,font=font(16,True)); d.text((x+(w-(bb[2]-bb[0]))/2,y+16),label,font=font(16,True),fill=fg)

def navbar(d, active="Home"):
    d.rectangle((0,1115,W,H),fill=WHITE)
    items=[("H","Home"),("T","Translate"),("L","Lens"),("S","Settings")]
    for i,(ic,label) in enumerate(items):
        x=68+i*134; col=BLUE if active==label else MUTED
        d.text((x,1125),ic,font=font(22,True),fill=col)
        bb=d.textbbox((0,0),label,font=font(11)); d.text((x-8,1160),label,font=font(11),fill=col)

def page_home():
    im,d=base("Home","SwiftLens","Translate messages, text and speech.",True)
    card(d,150,300,BLUE)
    text(d,44,176,"TRANSLATE ANYWHERE",12,"#DCE9FF",True)
    text(d,44,216,"Understand every",27,WHITE,True); text(d,44,251,"conversation.",27,WHITE,True)
    rr(d,(420,190,486,256),"#5794F4",34); text(d,441,211,"T",28,WHITE,True)
    button(d,354,"L   Set up Translate Lens",x=42,w=430,fill=WHITE,fg=BLUE)
    text(d,24,478,"QUICK TRANSLATE",12,MUTED,True)
    cards=[("T","Text","Type or paste"),("P","Photo","Choose an image"),("V","Voice","Speak to translate"),("C","Chat apps","Use the lens in chats")]
    for i,(ic,t,note) in enumerate(cards):
        row=i//2; col=i%2; x=20+col*255; y=518+row*175
        rr(d,(x,y,x+245,y+158),WHITE,18); rr(d,(x+16,y+16,x+58,y+58),PALE,12)
        text(d,x+28,y+23,ic,24,BLUE,True); text(d,x+16,y+79,t,17,NAVY,True); text(d,x+16,y+111,note,12,MUTED)
    card(d,870,82); text(d,40,887,"S",22,BLUE,True); text(d,80,884,"Video subtitles",16,NAVY,True); text(d,80,909,"Translate accessible captions",12,MUTED); text(d,482,888,"›",25,MUTED)
    text(d,40,1000,"L",20,BLUE,True); text(d,78,1003,"English  <->  简体中文",16,NAVY,True); text(d,480,1001,">",24,MUTED)
    navbar(d,"Home"); return im

def page_translate():
    im,d=base("Translate","Text translation","Type, paste, speak or choose a photo.",True)
    card(d,160,68); text(d,110,182,"English",16,NAVY,True); text(d,250,178,"<>",20,BLUE,True); text(d,330,182,"简体中文",16,NAVY,True)
    card(d,246,300); text(d,42,266,"Enter text",16,"#A0AAB9"); text(d,42,322,"Type or paste text here",17,"#A0AAB9")
    rr(d,(466,268,506,308),PALE,12); text(d,478,274,"×",23,BLUE,True)
    button(d,570,"P   Photo",x=20,w=242,fill=WHITE,fg=BLUE); button(d,570,"V   Voice",x=278,w=242,fill=WHITE,fg=BLUE)
    button(d,644,"T   Translate")
    card(d,722,210); text(d,42,742,"TRANSLATION",11,MUTED,True); text(d,42,782,"Translation appears here",19,NAVY,True); wrap(d,42,823,"Offline translation is available after the Chinese model is downloaded.",440,13,MUTED)
    navbar(d,"Translate"); return im

def page_lens():
    im,d=base("Lens","Screen translator","Translate text inside other apps with a floating lens.",True)
    card(d,166,474); rr(d,(231,196,309,274),PALE,39); text(d,251,211,"L",37,BLUE,True)
    text(d,142,303,"Translate Lens",21,NAVY,True)
    wrap(d,52,346,"Drag the bubble onto a message, post or subtitle. SwiftLens reads the nearest accessibility text node and shows the translation beside it.",436,14,MUTED)
    button(d,500,"Enable Accessibility Service",x=42,w=456)
    button(d,568,"View setup steps",x=42,w=456,fill=WHITE,fg=BLUE)
    card(d,668,158); text(d,42,686,"D",22,BLUE,True); text(d,82,687,"Offline language models",16,NAVY,True); text(d,82,714,"English + Chinese · Wi-Fi download",12,MUTED)
    rr(d,(82,750,300,790),PALE,10); text(d,103,761,"Manage packs in SwiftLens",12,BLUE,True)
    wrap(d,24,834,"Canvas-rendered, image-only and protected text may not be available through Accessibility.",480,13,MUTED)
    navbar(d,"Lens"); return im

def page_setup():
    im,d=base("Setup","Set up Translate Lens","Allow SwiftLens to read only the text you point the lens at.")
    lines=["Open Accessibility settings","Choose SwiftLens and turn it on","Return here, then drag the floating lens onto text"]
    y=170
    for i,line in enumerate(lines,1):
        card(d,y,105); rr(d,(40,y+32,76,y+68),PALE,18); text(d,52,y+39,str(i),17,BLUE,True); wrap(d,94,y+34,line,395,15,NAVY,True); y+=125
    button(d,580,"Open Accessibility settings"); text(d,169,653,"I’ll do this later",15,BLUE,True)
    card(d,716,110); wrap(d,42,741,"SwiftLens does not use MediaProjection, screen recording or screenshots.",450,14,MUTED)
    return im

def page_photo():
    im,d=base("Photo","Photo translator","Select a photo and recognize its text on this device.")
    card(d,166,480); rr(d,(224,290,316,382),PALE,22); text(d,256,309,"P",43,BLUE,True)
    text(d,188,410,"Choose photo",18,NAVY,True); text(d,180,443,"From your photo library",13,MUTED)
    button(d,676,"P   Select an image"); text(d,24,759,"Choose a photo containing text.",14,MUTED)
    wrap(d,24,814,"Only the image you choose is processed. This does not capture your display.",480,13,MUTED)
    text(d,220,946,"Back to home",14,BLUE,True); return im

def page_voice():
    im,d=base("Voice","Voice translator","Speak and translate with on-device speech recognition.")
    card(d,174,440); text(d,183,203,"English > 简体中文",15,BLUE,True)
    rr(d,(220,292,320,392),BLUE,50); text(d,257,318,"V",40,WHITE,True)
    text(d,209,424,"Tap to speak",17,NAVY,True); text(d,147,462,"Tap the microphone to dictate.",13,MUTED)
    wrap(d,24,666,"Microphone permission is requested when you tap the microphone. Audio recognition stays on device when supported.",480,13,MUTED)
    text(d,220,824,"Back to home",14,BLUE,True); return im

def page_chat():
    im,d=base("Chat","Chat translator","Use the floating lens inside your messaging apps.")
    apps=["WhatsApp","Instagram","Telegram","Messenger","Discord","Other apps"]
    y=164
    for app in apps:
        card(d,y,76); rr(d,(36,y+18,76,y+58),PALE,12); text(d,48,y+26,"C",20,BLUE,True); text(d,92,y+18,app,16,NAVY,True); text(d,92,y+43,"Drag the lens over a message",12,MUTED); text(d,480,y+21,"›",24,MUTED); y+=88
    button(d,724,"Set up Translate Lens")
    wrap(d,24,804,"This build translates exposed text nodes one at a time. It does not read notifications or monitor chats in the background.",480,13,MUTED)
    return im

def page_subtitles():
    im,d=base("Subtitles","Subtitle translator","Translate captions when the video app exposes their text.")
    card(d,174,360); text(d,42,201,"S",34,BLUE,True); text(d,42,260,"Point the lens at a caption",20,NAVY,True)
    wrap(d,42,310,"Open a video with accessible captions, enable Translate Lens, then drag it onto a caption. SwiftLens reads the caption node exposed by that app.",445,15,MUTED)
    wrap(d,42,432,"Video audio transcription and screen capture are not part of this build.",440,13,MUTED)
    button(d,578,"Set up Translate Lens"); return im

def page_settings():
    im,d=base("Settings","Settings",None,True)
    rows=[("L","Translation languages","English > 简体中文"),("L","Translate Lens","Accessibility and floating bubble"),("D","Offline models","Select language packs"),("P","Privacy","How SwiftLens reads and translates text"),("i","About","SwiftLens for Android · 0.5.0")]
    y=157
    for icon,title,note in rows:
        card(d,y,88); text(d,39,y+28,icon,24,BLUE,True); text(d,84,y+16,title,15,NAVY,True); text(d,84,y+46,note,11,MUTED); text(d,480,y+25,"›",24,MUTED); y+=101
    text(d,24,700,"No ads · No billing · No screen capture",13,MUTED); navbar(d,"Settings"); return im

def page_languages():
    im,d=base("Languages","Translation languages","Choose the direction for text, voice and the floating lens.")
    for y,label,checked in [(170,"English > 简体中文",True),(268,"简体中文 > English",False)]:
        card(d,y,78); text(d,44,y+25,label,17,NAVY,True)
        if checked:
            d.ellipse((462,y+23,486,y+47),outline=BLUE,width=2)
            d.line((468,y+35,474,y+41,481,y+29),fill=BLUE,width=2)
    return im

def page_privacy():
    im,d=base("Privacy","Privacy",None,True)
    card(d,162,444)
    text(d,42,186,"Text access",16,NAVY,True)
    wrap(d,42,224,"The lens reads the Accessibility text node closest to the position where you drop it. It does not read notifications or scan the screen in the background.",440,14,MUTED)
    text(d,42,344,"On-device processing",16,NAVY,True)
    wrap(d,42,382,"Downloaded translation models and selected-photo OCR run on device. Speech recognition uses the on-device recognizer when available.",440,14,MUTED)
    text(d,42,490,"No display capture",16,NAVY,True)
    wrap(d,42,528,"SwiftLens does not use MediaProjection, screen recording or screenshots. It includes no ads or billing.",440,14,MUTED)
    text(d,24,650,"Accessibility can expose sensitive text. Enable the service only if you want to use Translate Lens.",14,MUTED)
    navbar(d,"Settings"); return im

def page_about():
    im,d=base("About","About SwiftLens",None,True)
    card(d,166,270)
    rr(d,(228,198,312,282),PALE,42); text(d,254,219,"L",37,BLUE,True)
    text(d,208,309,"SwiftLens for Android",20,NAVY,True)
    text(d,212,345,"Version 0.5.0 · Android 16 target",13,MUTED)
    card(d,462,150)
    text(d,42,486,"English <-> 简体中文",16,NAVY,True)
    wrap(d,42,528,"Text, photo, voice and Accessibility-based Translate Lens.",440,14,MUTED)
    text(d,24,662,"Offline model download requires an initial Wi-Fi connection.",13,MUTED)
    navbar(d,"Settings"); return im

def page_models():
    im,d=base("Models","Offline models","Select individual language packs for offline translation.")
    for y,label,note,ready in [(174,"English","English model · Downloaded",True),(278,"简体中文","Simplified Chinese model · Not installed",False)]:
        card(d,y,84); rr(d,(42,y+27,66,y+51),WHITE,5,outline=BLUE,width=2)
        if ready:
            d.line((47,y+39,52,y+45,62,y+32),fill=BLUE,width=3)
        text(d,86,y+19,label,16,NAVY,True); text(d,86,y+49,note,12,MUTED)
    card(d,388,164); text(d,44,412,"Both packs needed · English / Chinese",16,NAVY,True)
    wrap(d,44,450,"Select either model independently. Downloads require Wi-Fi and translations run locally after installation.",444,14,MUTED)
    button(d,590,"Download selected packs")
    button(d,660,"Remove selected packs",fill=WHITE,fg=BLUE)
    text(d,24,750,"Each language pack can be managed separately.",13,MUTED)
    return im

def modal_background(page):
    im=page().convert("RGBA")
    shade=Image.new("RGBA",(W,H),(10,20,35,145))
    return Image.alpha_composite(im,shade).convert("RGB")

def page_method():
    im=modal_background(page_translate); d=ImageDraw.Draw(im)
    rr(d,(34,300,506,850),WHITE,24)
    text(d,58,326,"Choose translation method",22,NAVY,True)
    text(d,58,367,"Offline stays on this device. Online opens a",13,MUTED); text(d,58,389,"provider you choose.",13,MUTED)
    for y,label,selected in [(444,"Offline · ML Kit (on device)",True),(510,"Online · provider website",False)]:
        d.ellipse((61,y,83,y+22),outline=BLUE if selected else MUTED,width=2)
        if selected: d.ellipse((67,y+6,77,y+16),fill=BLUE)
        text(d,96,y,label,15,NAVY,True)
    text(d,60,582,"ONLINE PLATFORM",11,MUTED,True)
    for y,label,checked in [(619,"Google Translate",True),(671,"DeepL",False),(723,"Microsoft Bing Translator",False)]:
        d.ellipse((64,y,84,y+20),outline=BLUE if checked else MUTED,width=2)
        if checked: d.ellipse((69,y+5,79,y+15),fill=BLUE)
        text(d,97,y,label,14,NAVY)
    rr(d,(62,782,236,830),"#F0F3F7",14); text(d,119,797,"Cancel",14,BLUE,True)
    rr(d,(252,782,478,830),BLUE,14); text(d,324,797,"Continue",14,WHITE,True)
    return im

def page_consent():
    im=modal_background(page_translate); d=ImageDraw.Draw(im)
    rr(d,(34,360,506,810),WHITE,24)
    text(d,58,391,"Open Google Translate?",21,NAVY,True)
    wrap(d,58,443,"The text you entered will be placed in Google Translate and may be sent to that provider for online processing. SwiftLens will not send it until you confirm here.",410,15,MUTED)
    rr(d,(58,555,482,625),"#F4F7FB",14)
    text(d,74,577,"Text: How are you?",14,NAVY)
    rr(d,(62,727,227,777),"#F0F3F7",14); text(d,119,743,"Back",14,BLUE,True)
    rr(d,(245,727,482,777),BLUE,14); text(d,293,743,"Agree & open",14,WHITE,True)
    return im

pages={"Home":page_home,"Translate":page_translate,"Method":page_method,"Online consent":page_consent,"Lens":page_lens,"Models":page_models,"Setup":page_setup,"Photo":page_photo,"Voice":page_voice,"Chat":page_chat,"Subtitles":page_subtitles,"Settings":page_settings,"Languages":page_languages,"Privacy":page_privacy,"About":page_about}
for name,render in pages.items():
    render().save(OUT/f"{name}.png")

# A compact contact sheet for quick visual review.
thumbs=[]
for name in pages:
    im=Image.open(OUT/f"{name}.png").resize((216,480))
    thumbs.append((name,im))
sheet=Image.new("RGB",(216*4,510*4+60),"#E8EDF4"); sd=ImageDraw.Draw(sheet)
for idx,(name,im) in enumerate(thumbs):
    x=(idx%4)*216; y=(idx//4)*510
    sheet.paste(im,(x,y+25)); sd.text((x+10,y+4),name,font=font(14,True),fill=NAVY)
sd.text((10,2050),"Layout render preview · not an Android device screenshot",font=font(12),fill=MUTED)
sheet.save(OUT/"ContactSheet.png")

