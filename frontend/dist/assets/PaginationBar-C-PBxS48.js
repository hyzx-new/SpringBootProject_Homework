import{c as m,o as c,a as d,b as s,t as o,F as N,q as x,e as v,u as h,x as l}from"./index-DGasrsU_.js";/**
 * @license lucide-vue-next v0.468.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const k=m("ChevronLeftIcon",[["path",{d:"m15 18-6-6 6-6",key:"1wnfg3"}]]);/**
 * @license lucide-vue-next v0.468.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const C=m("ChevronRightIcon",[["path",{d:"m9 18 6-6-6-6",key:"mthhwq"}]]),q={class:"pagination"},M={class:"pagination-info"},B={class:"pagination-controls"},L=["value"],P=["value"],F=["disabled"],I={class:"page-index"},O=["disabled"],w={__name:"PaginationBar",props:{page:{type:Number,required:!0},size:{type:Number,required:!0},total:{type:Number,required:!0},pages:{type:Number,required:!0},sizeOptions:{type:Array,default:()=>[10,20,50]}},emits:["change"],setup(r,{emit:b}){const t=r,p=b,a=l(()=>Math.max(t.page,1)),u=l(()=>Math.max(t.pages,1)),_=l(()=>t.total===0?0:(a.value-1)*t.size+1),z=l(()=>Math.min(a.value*t.size,t.total)),f=l(()=>{const n=t.sizeOptions.map(e=>Number(e)).filter(e=>Number.isFinite(e)&&e>0);return n.length>0?n:[10,20,50]});function g(n){const e=Math.min(Math.max(n,1),u.value);e!==a.value&&p("change",{page:e,size:t.size})}function y(n){p("change",{page:1,size:Number(n.target.value)})}return(n,e)=>(c(),d("div",q,[s("div",M,[s("span",null,"共 "+o(r.total)+" 条",1),s("span",null,o(_.value)+"-"+o(z.value),1)]),s("div",B,[s("select",{class:"select compact",value:r.size,onChange:y},[(c(!0),d(N,null,x(f.value,i=>(c(),d("option",{key:i,value:i},o(i)+" 条/页",9,P))),128))],40,L),s("button",{class:"btn icon",disabled:a.value<=1,title:"上一页",onClick:e[0]||(e[0]=i=>g(a.value-1))},[v(h(k),{size:17})],8,F),s("span",I,o(a.value)+" / "+o(u.value),1),s("button",{class:"btn icon",disabled:a.value>=u.value,title:"下一页",onClick:e[1]||(e[1]=i=>g(a.value+1))},[v(h(C),{size:17})],8,O)])]))}};export{w as _};
