"use client";
import {useEffect,useRef} from "react";

/** Keep the selected step visible in the horizontally scrolling mobile navigator. */
export function useStepNavigation(step:number){
 const navigation=useRef<HTMLElement>(null);
 useEffect(()=>{
  const nav=navigation.current;if(!nav)return;
  const reveal=()=>{const selected=nav.querySelector<HTMLElement>('[aria-current="step"]');if(!selected||nav.scrollWidth<=nav.clientWidth)return;const current=selected.getBoundingClientRect(),frame=nav.getBoundingClientRect();nav.scrollLeft+=current.left-frame.left-(nav.clientWidth-current.width)/2;};
  reveal();const observer=new ResizeObserver(reveal);observer.observe(nav);return()=>observer.disconnect();
 },[step]);
 return navigation;
}
