"""Draws Reverie's launcher icon (crescent moon + sparkles, black line on white) at all mipmap sizes."""
from PIL import Image,ImageDraw
import sys,os
S=1536  # supersample canvas (8x of 192)
def save(img,resdir):
    for d,px in (('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)):
        os.makedirs(f'{resdir}/mipmap-{d}',exist_ok=True)
        img.resize((px,px),Image.LANCZOS).save(f'{resdir}/mipmap-{d}/ic_launcher.png')
def base(): return Image.new('L',(S,S),255)
W=64  # stroke at supersample (= 8px at 192)
def reverie():
    im=base(); d=ImageDraw.Draw(im)
    # crescent: outline circle minus offset circle, drawn as thick arcs
    c=(S*0.47,S*0.52); r=S*0.30
    m=Image.new('L',(S,S),0); md=ImageDraw.Draw(m)
    md.ellipse([c[0]-r,c[1]-r,c[0]+r,c[1]+r],fill=255)
    oc=(c[0]+r*0.55,c[1]-r*0.35); md.ellipse([oc[0]-r*0.95,oc[1]-r*0.95,oc[0]+r*0.95,oc[1]+r*0.95],fill=0)
    inner=m.resize((S//4,S//4)).resize((S,S))
    from PIL import ImageFilter
    er=m.filter(ImageFilter.MinFilter(2*W//8*2+1)) if False else m.filter(ImageFilter.MinFilter(W*2+1))
    ring=Image.eval(m,lambda v:v)  # filled crescent
    outline=Image.composite(Image.new('L',(S,S),0),Image.new('L',(S,S),255),Image.eval(Image.composite(Image.new('L',(S,S),0),m,er),lambda v:v))
    im.paste(0,mask=Image.composite(Image.new('L',(S,S),0),m,er))
    d=ImageDraw.Draw(im)
    def star(x,y,s):
        d.line([x-s,y,x+s,y],fill=0,width=W); d.line([x,y-s,x,y+s],fill=0,width=W)
    star(S*0.72,S*0.30,S*0.07); star(S*0.80,S*0.55,S*0.045)
    return im
# Usage: python3 tools_icons.py res   (writes res/mipmap-*/ic_launcher.png)
res=sys.argv[1] if len(sys.argv)>1 else 'res'
save(reverie(),res)
