export function rows(d){if(Array.isArray(d))return d;return d?.content||d?.items||d?.records||d?.data||[]}
export function bytes(n=0){n=Number(n);if(!Number.isFinite(n)||n<0)return '—';if(n<1024)return `${n} B`;let i=-1;do{n/=1024;i++;}while(n>=1024&&i<3);return `${n.toFixed(n>=10?1:2)} ${['KB','MB','GB','TB'][i]}`}
export function date(v){if(!v)return '—';const d=new Date(v);return Number.isNaN(d.getTime())?'—':new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short'}).format(d)}
export function ext(n=''){return n.includes('.')?n.split('.').pop().toUpperCase():'FILE'}
