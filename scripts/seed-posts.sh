#!/usr/bin/env bash
#
# Seeds the board with sample posts so the feed has something in it while you
# build the UI. Safe to run more than once — it just adds more posts.
#
# Usage:
#   bash seed-posts.sh
#
# Requires the backend to be running.

set -e

API="http://localhost:8081"
EMAIL="test@example.com"
PASSWORD="testpass123"

echo "Signing in as $EMAIL..."

LOGIN_RESPONSE=$(curl -s -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

# Pull the token out of the JSON without needing jq installed.
TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "Could not sign in. Response was:"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

echo "Signed in."
echo

post() {
  local payload="$1"
  local title
  title=$(echo "$payload" | sed -n 's/.*"title":"\([^"]*\)".*/\1/p')

  local response
  response=$(curl -s -X POST "$API/api/posts" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "$payload")

  if echo "$response" | grep -q '"id"'; then
    echo "  ok   $title"
  else
    echo "  FAIL $title"
    echo "       $response"
  fi
}

echo "Creating posts..."

post '{"title":"No working streetlights on the road behind the station","body":"That stretch has been dark for about three weeks now. I take it most evenings around 8 and it has started to feel genuinely unsafe. Has anyone here actually managed to get the ward office to act on something like this? Wondering whether a written complaint does more than a phone call.","categorySlug":"harassment","areaName":"Andheri West","city":"Mumbai","state":"Maharashtra","latitude":19.136751,"longitude":72.826508}'

post '{"title":"Manager keeps scheduling one-on-ones after 8pm","body":"Every other week it is a last-minute meeting request for after hours, always just the two of us, always in his cabin once the floor has emptied out. Nothing has happened. But I have started making excuses and I am tired of it. Is there a way to raise this with HR that does not turn into a formal complaint straight away?","categorySlug":"workplace","areaName":"Bandra Kurla Complex","city":"Mumbai","state":"Maharashtra","latitude":19.066523,"longitude":72.865987}'

post '{"title":"Free pad distribution at the community centre this Saturday","body":"Sharing in case it helps someone. A local group is running a distribution drive from 10am to 2pm this Saturday. No registration or ID needed as far as I know, you just show up. They mentioned reusable cloth pads as well as regular ones.","categorySlug":"health","areaName":"Dadar","city":"Mumbai","state":"Maharashtra","latitude":19.018255,"longitude":72.844387}'

post '{"title":"How do I find out if a rental agreement clause is even legal","body":"My landlord has added a clause saying no guests after 9pm and that he can enter to inspect whenever he likes. I signed it because I needed the place quickly. Is something like this actually enforceable, and is there free legal advice anywhere for tenants?","categorySlug":"legal","areaName":"Kothrud","city":"Pune","state":"Maharashtra","latitude":18.507147,"longitude":73.807182}'

post '{"title":"Six months into living alone and the evenings are the hardest","body":"I moved cities for work and I do like the job. But between about 7pm and bedtime the flat is very quiet and I end up scrolling until I feel worse. I am not in crisis, I just did not expect it to be this specific about the time of day. Does this settle down eventually?","categorySlug":"mental-health","areaName":"Koramangala","city":"Bengaluru","state":"Karnataka","latitude":12.934533,"longitude":77.626579}'

post '{"title":"Sister needs to leave her marriage but has no money of her own","body":"She has no separate bank account and has not worked since the wedding. She is ready to go but every practical step needs money she cannot access without him noticing. Has anyone been through this or helped someone through it? I mostly need to know what order to do things in.","categorySlug":"domestic","areaName":"Vashi","city":"Navi Mumbai","state":"Maharashtra","latitude":19.077391,"longitude":72.998589}'

post '{"title":"Interest-free loan schemes for women starting small businesses","body":"I have been running a tiffin service out of my kitchen for two years and want to take a small shop. Banks want collateral I do not have. Someone mentioned there are government schemes specifically for women but the websites are hard to make sense of. Has anyone actually got one of these?","categorySlug":"financial","areaName":"Shivaji Nagar","city":"Pune","state":"Maharashtra","latitude":18.530822,"longitude":73.851654}'

post '{"title":"Looking for a women-only PG in Powai, recommendations welcome","body":"Moving next month for work. Budget is reasonably flexible but I care much more about the area feeling safe at night and the place actually being what it says in the listing. If you have lived somewhere good, or somewhere to avoid, I would really appreciate hearing about it.","categorySlug":"general","areaName":"Powai","city":"Mumbai","state":"Maharashtra","latitude":19.116669,"longitude":72.905998}'

post '{"title":"Auto driver followed me after I refused the fare","body":"He quoted double the meter, I said no and walked, and he drove alongside me for most of the way to the next junction. Nothing came of it but I noted the number. Is filing a complaint with the RTO worth doing, or does it just disappear into a file somewhere?","categorySlug":"harassment","areaName":"Indiranagar","city":"Bengaluru","state":"Karnataka","latitude":12.978207,"longitude":77.640558}'

post '{"title":"Anyone tried a menstrual cup after years of pads","body":"Thinking of switching, mostly for cost reasons over the long run. The reviews are all either very enthusiastic or very discouraging and I cannot tell what is normal. How long did it take before it stopped being awkward?","categorySlug":"health","areaName":"Salt Lake","city":"Kolkata","state":"West Bengal","latitude":22.580364,"longitude":88.417397}'

echo
echo "Done. Check the board:"
echo "  curl \"$API/api/posts\""
echo "  curl \"$API/api/posts?city=Mumbai\""
echo "  curl \"$API/api/posts?category=health\""